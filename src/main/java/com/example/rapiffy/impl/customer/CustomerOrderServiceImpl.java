package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.repos.CustomerAddressRepository;
import com.example.rapiffy.services.customer.CustomerCartService;
import com.example.rapiffy.services.customer.CustomerOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final ParentOrderRepository parentOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerCartService cartService;
    private final CustomerAddressRepository customerAddressRepository;

    @Override
    @Transactional
    public ParentOrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if ("DELIVERY".equalsIgnoreCase(request.getDeliveryType())) {
            // If no address provided → use default saved address
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                request.setDeliveryAddress(
                    customerAddressRepository.findByCustomerAndIsDefault(customer, true)
                        .map(a -> String.join(", ",
                            nullSafe(a.getAddress().getAddressLine1()),
                            nullSafe(a.getAddress().getCity()),
                            nullSafe(a.getAddress().getState()),
                            nullSafe(a.getAddress().getPinCode())))
                        .orElseThrow(() -> new ApiException(
                            "No delivery address provided and no default address saved.", HttpStatus.BAD_REQUEST))
                );
            }
        }

        // Build parent order
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = now.format(DateTimeFormatter.ofPattern("HHmm"));
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ParentOrder parentOrder = new ParentOrder();
        parentOrder.setCustomer(customer);
        parentOrder.setDeliveryType(request.getDeliveryType().toUpperCase());
        parentOrder.setDeliveryAddress(request.getDeliveryAddress());
        parentOrder.setDeliveryInstruction(request.getDeliveryInstruction());
        parentOrder.setOrderNumber("PO-" + dateStr + "" + timeStr + "-" + uniqueSuffix);
        parentOrder.setStatus(OrderStatus.PAYMENT_PENDING);
        log.info("Creating order for user: " + parentOrder.getOrderNumber());
        parentOrder.setSubtotal(0.0);
        parentOrder.setTotalGst(0.0);
        parentOrder.setTotalAmount(0.0);
        parentOrder.setPaymentMethod(request.getPaymentMethod());
        ParentOrder savedParent = parentOrderRepository.save(parentOrder);

        // Resolve every item — support both ShopProduct and ProductVariant
        List<ShopProduct> resolvedProducts = new ArrayList<>();
        List<ProductVariant> resolvedVariants = new ArrayList<>();
        List<PlaceOrderItemRequest> resolvedRequests = new ArrayList<>();

        for (PlaceOrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getShopProductId() == null)
                throw new ApiException("shopProductId is required for each item", HttpStatus.BAD_REQUEST);

            ShopProduct sp = shopProductRepository.findById(itemReq.getShopProductId()).orElse(null);
            ProductVariant variant = null;

            if (sp == null || sp.isHasVariants()) {
                variant = productVariantRepository.findByShopProductId(itemReq.getShopProductId())
                        .orElseThrow(() -> new ApiException("Product not found: " + itemReq.getShopProductId(), HttpStatus.NOT_FOUND));
                if (!variant.isActive())
                    throw new ApiException("Product not available: " + variant.getVariantName(), HttpStatus.BAD_REQUEST);
                if (variant.getStockQuantity() < itemReq.getQuantity())
                    throw new ApiException("Insufficient stock for: " + variant.getVariantName(), HttpStatus.BAD_REQUEST);
                sp = variant.getParentShopProduct();
            } else {
                if (!sp.isActive())
                    throw new ApiException("Product not available: " + sp.getProductName(), HttpStatus.BAD_REQUEST);
                if (sp.getStockQuantity() < itemReq.getQuantity())
                    throw new ApiException("Insufficient stock for: " + sp.getProductName(), HttpStatus.BAD_REQUEST);
            }

            resolvedProducts.add(sp);
            resolvedVariants.add(variant); // null if plain product
            resolvedRequests.add(itemReq);
        }

        // Group by shop
        Map<Long, List<Integer>> byShop = new LinkedHashMap<>();
        for (int i = 0; i < resolvedProducts.size(); i++) {
            Long shopId = resolvedProducts.get(i).getShop().getId();
            byShop.computeIfAbsent(shopId, k -> new ArrayList<>()).add(i);
        }

        double grandSubtotal = 0.0;
        double grandGst = 0.0;
        List<Order> subOrders = new ArrayList<>();
        int shopIndex = 1;

        for (Map.Entry<Long, List<Integer>> entry : byShop.entrySet()) {
            Profile shop = resolvedProducts.get(entry.getValue().get(0)).getShop();
            List<OrderItem> orderItems = new ArrayList<>();
            double subTotal = 0.0;
            double subGst = 0.0;

            for (int idx : entry.getValue()) {
                ShopProduct sp = resolvedProducts.get(idx);
                ProductVariant variant = resolvedVariants.get(idx);
                PlaceOrderItemRequest itemReq = resolvedRequests.get(idx);

                String productName  = variant != null ? variant.getVariantName()                    : sp.getProductName();
                String brand        = variant != null ? variant.getBrand()                           : sp.getBrand();
                String unit         = variant != null ? sp.getUnit()                                 : sp.getUnit();
                String unitValue    = variant != null ? sp.getUnitValue()                            : sp.getUnitValue();
                String imageUrl     = variant != null ? variant.getImageUrl()                        : sp.getImageUrl();
                Double mrp          = variant != null ? variant.getMrp()                             : sp.getMrp();
                Double sellingPrice = variant != null ? variant.getSellingPrice()                    : sp.getSellingPrice();
                String gstSlab      = variant != null ? variant.getGstSlab()                         : sp.getGstSlab();

                double gstRate      = parseGstRate(gstSlab);
                double lineSubtotal = sellingPrice * itemReq.getQuantity();
                double gstAmount    = Math.round(lineSubtotal * gstRate * 100.0) / 100.0;

                subTotal += lineSubtotal;
                subGst   += gstAmount;

                OrderItem item = new OrderItem();
                item.setShopProduct(sp);
                item.setVariantId(variant != null ? variant.getId() : null);
                item.setProductName(productName);
                item.setBrand(brand);
                item.setUnit(unit);
                item.setUnitValue(unitValue);
                item.setImageUrl(imageUrl);
                item.setMrp(mrp);
                item.setSellingPrice(sellingPrice);
                item.setQuantity(itemReq.getQuantity());
                item.setGstSlab(gstSlab);
                item.setGstAmount(gstAmount);
                item.setLineTotal(Math.round((lineSubtotal + gstAmount) * 100.0) / 100.0);
                orderItems.add(item);
            }

            double subOrderTotal = Math.round((subTotal + subGst) * 100.0) / 100.0;

            Order subOrder = new Order();
            subOrder.setParentOrder(savedParent);
            subOrder.setCustomer(customer);
            subOrder.setShop(shop);
            subOrder.setOrderNumber(savedParent.getOrderNumber() + "-S" + shopIndex++);
            subOrder.setDeliveryType(request.getDeliveryType().toUpperCase());
            subOrder.setDeliveryAddress(request.getDeliveryAddress());
            subOrder.setSubtotal(Math.round(subTotal * 100.0) / 100.0);
            subOrder.setTotalGst(Math.round(subGst * 100.0) / 100.0);
            subOrder.setDeliveryCharge(0.0);
            subOrder.setTotalAmount(subOrderTotal);
            subOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            Order savedSubOrder = orderRepository.save(subOrder);

            orderItems.forEach(item -> item.setOrder(savedSubOrder));
            orderItemRepository.saveAll(orderItems);
            savedSubOrder.setItems(orderItems);
            subOrders.add(savedSubOrder);

            grandSubtotal += subTotal;
            grandGst += subGst;
        }

        // Update parent totals
        savedParent.setSubtotal(Math.round(grandSubtotal * 100.0) / 100.0);
        savedParent.setTotalGst(Math.round(grandGst * 100.0) / 100.0);
        savedParent.setTotalAmount(Math.round((grandSubtotal + grandGst) * 100.0) / 100.0);
        savedParent.setSubOrders(subOrders);

        // COD → directly PENDING so admin can see it, no payment needed
        // UPI/CARD/etc → stays PAYMENT_PENDING until payment is verified
        if (request.getPaymentMethod() == com.example.rapiffy.enums.PaymentMethod.COD) {
            savedParent.setStatus(OrderStatus.PENDING);
            savedParent.setPaymentStatus(com.example.rapiffy.enums.PaymentStatus.PENDING);
            subOrders.forEach(o -> {
                o.setStatus(OrderStatus.PENDING);
                orderRepository.save(o);
            });
        }

        parentOrderRepository.save(savedParent);

        // COD → clear cart immediately (no payment step follows)
        if (request.getPaymentMethod() == com.example.rapiffy.enums.PaymentMethod.COD) {
            try { cartService.clearCart(userId); } catch (Exception ignored) {}
        }
        // UPI/Online → cart cleared only after payment is verified (in verifyPayment)

        return toParentOrderResponse(savedParent);
    }

    @Override
    public List<CustomerOrderSummaryResponse> getMyOrders(Long userId) {
        return parentOrderRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public ParentOrderResponse getOrderDetail(Long userId, Long parentOrderId) {
        ParentOrder parentOrder = parentOrderRepository.findById(parentOrderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (!parentOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        return toParentOrderResponse(parentOrder);
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private double parseGstRate(String gstSlab) {
        if (gstSlab == null || gstSlab.isBlank()) return 0.0;
        try {
            return Double.parseDouble(gstSlab.replace("%", "").trim()) / 100.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private CustomerOrderSummaryResponse toSummary(ParentOrder po) {
        CustomerOrderSummaryResponse r = new CustomerOrderSummaryResponse();
        r.setOrderId(po.getId());
        r.setOrderNumber(po.getOrderNumber());
        r.setTotalItems(po.getSubOrders().stream().mapToInt(o -> o.getItems().size()).sum());
        r.setThumbnailImage(po.getSubOrders().stream()
                .flatMap(o -> o.getItems().stream())
                .map(OrderItem::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null));
        r.setSubtotal(po.getSubtotal());
        r.setTotalGst(po.getTotalGst());
        r.setDeliveryCharge(0.0);
        r.setTotalAmount(po.getTotalAmount());
        r.setDeliveryType(po.getDeliveryType());
        r.setCreatedAt(po.getCreatedAt());
        return r;
    }

    private ParentOrderResponse toParentOrderResponse(ParentOrder po) {
        ParentOrderResponse r = new ParentOrderResponse();
        r.setParentOrderId(po.getId());
        r.setOrderNumber(po.getOrderNumber());
        r.setDeliveryType(po.getDeliveryType());
        r.setDeliveryAddress(po.getDeliveryAddress());
        r.setDeliveryInstruction(po.getDeliveryInstruction());
        r.setSubtotal(po.getSubtotal());
        r.setTotalGst(po.getTotalGst());
        r.setTotalAmount(po.getTotalAmount());
        r.setStatus(po.getStatus());
        r.setCreatedAt(po.getCreatedAt());
        r.setUpdatedAt(po.getUpdatedAt());
        r.setSubOrders(po.getSubOrders().stream().map(this::toSubOrderResponse).toList());
        return r;
    }

    @Override
    public CustomerInvoiceResponse getSubOrderInvoice(Long userId, Long parentOrderId, Long subOrderId) {
        ParentOrder parentOrder = parentOrderRepository.findById(parentOrderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (!parentOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        Order subOrder = parentOrder.getSubOrders().stream()
                .filter(o -> o.getId().equals(subOrderId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Sub-order not found", HttpStatus.NOT_FOUND));

        if (subOrder.getStatus() == OrderStatus.PAYMENT_PENDING || subOrder.getStatus() == OrderStatus.PENDING)
            throw new ApiException("Invoice not available yet. Order has not been confirmed by the shop.", HttpStatus.BAD_REQUEST);

        Profile shop = subOrder.getShop();
        CustomerInvoiceResponse.ShopInvoiceSection section = new CustomerInvoiceResponse.ShopInvoiceSection();
        section.setShopName(shop.getShopName());
        if (shop.getAddress() != null)
            section.setShopAddress(String.join(", ",
                    nullSafe(shop.getAddress().getAddressLine1()),
                    nullSafe(shop.getAddress().getCity()),
                    nullSafe(shop.getAddress().getPinCode())));
        if (shop.getPhoneNumber() != null)
            section.setShopPhone(shop.getPhoneNumber().getPhoneNumber());
        section.setShopTotal(subOrder.getTotalAmount());
        section.setItems(subOrder.getItems().stream().map(item -> {
            OrderItemResponse i = new OrderItemResponse();
            i.setOrderItemId(item.getId());
            i.setProductName(item.getProductName());
            i.setBrand(item.getBrand());
            i.setUnit(item.getUnit());
            i.setUnitValue(item.getUnitValue());
            i.setMrp(item.getMrp());
            i.setSellingPrice(item.getSellingPrice());
            i.setQuantity(item.getQuantity());
            i.setGstSlab(item.getGstSlab());
            i.setGstAmount(item.getGstAmount());
            i.setLineTotal(item.getLineTotal());
            return i;
        }).toList());

        CustomerInvoiceResponse r = new CustomerInvoiceResponse();
        r.setOrderNumber(subOrder.getOrderNumber());
        r.setOrderDate(parentOrder.getCreatedAt());
        r.setCustomerPhone(parentOrder.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(subOrder.getDeliveryAddress());
        r.setDeliveryType(subOrder.getDeliveryType());
        r.setShops(List.of(section));
        r.setSubtotal(subOrder.getSubtotal());
        r.setTotalGst(subOrder.getTotalGst());
        r.setDeliveryCharge(subOrder.getDeliveryCharge());
        r.setTotalAmount(subOrder.getTotalAmount());
        return r;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private SubOrderResponse toSubOrderResponse(Order order) {
        SubOrderResponse r = new SubOrderResponse();
        r.setSubOrderId(order.getId());
        r.setSubOrderNumber(order.getOrderNumber());
        r.setShopName(order.getShop().getShopName());
        r.setSubtotal(order.getSubtotal());
        r.setTotalGst(order.getTotalGst());
        r.setTotalAmount(order.getTotalAmount());
        r.setStatus(order.getStatus());
        r.setItems(order.getItems().stream().map(item -> {
            OrderItemResponse i = new OrderItemResponse();
            i.setOrderItemId(item.getId());
            i.setShopProductId(item.getShopProduct() != null ? item.getShopProduct().getId() : null);
            i.setProductName(item.getProductName());
            i.setBrand(item.getBrand());
            i.setUnit(item.getUnit());
            i.setUnitValue(item.getUnitValue());
            i.setImageUrl(item.getImageUrl());
            i.setMrp(item.getMrp());
            i.setSellingPrice(item.getSellingPrice());
            i.setQuantity(item.getQuantity());
            i.setGstSlab(item.getGstSlab());
            i.setGstAmount(item.getGstAmount());
            i.setLineTotal(item.getLineTotal());
            return i;
        }).toList());
        return r;
    }
}
