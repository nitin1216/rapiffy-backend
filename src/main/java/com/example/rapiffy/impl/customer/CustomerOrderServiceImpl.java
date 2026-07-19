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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final ParentOrderRepository parentOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShopProductRepository shopProductRepository;
    private final ProfileRepository profileRepository;
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

        // Group items by shopId
        Map<Long, List<PlaceOrderItemRequest>> itemsByShop = request.getItems().stream()
                .collect(Collectors.groupingBy(PlaceOrderItemRequest::getShopId));

        // Build parent order first
        String dateStr = LocalDate.now().toString().replace("-", "");
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        ParentOrder parentOrder = new ParentOrder();
        parentOrder.setCustomer(customer);
        parentOrder.setDeliveryType(request.getDeliveryType().toUpperCase());
        parentOrder.setDeliveryAddress(request.getDeliveryAddress());
        parentOrder.setOrderNumber("PO-" + dateStr + "-" + uniqueSuffix);
        parentOrder.setStatus(OrderStatus.PENDING);
        parentOrder.setSubtotal(0.0);
        parentOrder.setTotalGst(0.0);
        parentOrder.setTotalAmount(0.0);
        ParentOrder savedParent = parentOrderRepository.save(parentOrder);

        double grandSubtotal = 0.0;
        double grandGst = 0.0;
        List<Order> subOrders = new ArrayList<>();
        int shopIndex = 1;

        // Create one sub-order per shop
        for (Map.Entry<Long, List<PlaceOrderItemRequest>> entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<PlaceOrderItemRequest> shopItems = entry.getValue();

            Profile shop = profileRepository.findById(shopId)
                    .orElseThrow(() -> new ApiException("Shop not found: " + shopId, HttpStatus.NOT_FOUND));

            List<OrderItem> orderItems = new ArrayList<>();
            double subTotal = 0.0;
            double subGst = 0.0;

            for (PlaceOrderItemRequest itemReq : shopItems) {
                ShopProduct sp = shopProductRepository.findById(itemReq.getShopProductId())
                        .orElseThrow(() -> new ApiException("Product not found: " + itemReq.getShopProductId(), HttpStatus.NOT_FOUND));

                if (!sp.getShop().getId().equals(shopId))
                    throw new ApiException("Product " + sp.getProductName() + " does not belong to shop " + shopId, HttpStatus.BAD_REQUEST);

                if (!sp.isActive())
                    throw new ApiException("Product not available: " + sp.getProductName(), HttpStatus.BAD_REQUEST);

                if (sp.getStockQuantity() < itemReq.getQuantity())
                    throw new ApiException("Insufficient stock for: " + sp.getProductName(), HttpStatus.BAD_REQUEST);

                double gstRate = parseGstRate(sp.getGstSlab());
                double lineSubtotal = sp.getSellingPrice() * itemReq.getQuantity();
                double gstAmount = Math.round(lineSubtotal * gstRate * 100.0) / 100.0;

                subTotal += lineSubtotal;
                subGst += gstAmount;

                OrderItem item = new OrderItem();
                item.setShopProduct(sp);
                item.setProductName(sp.getProductName());
                item.setBrand(sp.getBrand());
                item.setUnit(sp.getUnit());
                item.setUnitValue(sp.getUnitValue());
                item.setImageUrl(sp.getImageUrl());
                item.setMrp(sp.getMrp());
                item.setSellingPrice(sp.getSellingPrice());
                item.setQuantity(itemReq.getQuantity());
                item.setGstSlab(sp.getGstSlab());
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
            subOrder.setStatus(OrderStatus.PENDING);
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
        parentOrderRepository.save(savedParent);

        // Auto-clear cart after successful order
        cartService.clearCart(userId);

        return toParentOrderResponse(savedParent);
    }

    @Override
    public List<CustomerOrderSummaryResponse> getMyOrders(Long userId) {
        return parentOrderRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toSummary).toList();
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
        r.setShopName(po.getSubOrders().stream()
                .map(o -> o.getShop().getShopName())
                .collect(Collectors.joining(", ")));
        r.setTotalItems(po.getSubOrders().stream()
                .mapToInt(o -> o.getItems().size()).sum());
        r.setSubtotal(po.getSubtotal());
        r.setTotalGst(po.getTotalGst());
        r.setDeliveryCharge(0.0);
        r.setTotalAmount(po.getTotalAmount());
        r.setDeliveryType(po.getDeliveryType());
        r.setStatus(po.getStatus());
        r.setCreatedAt(po.getCreatedAt());
        return r;
    }

    private ParentOrderResponse toParentOrderResponse(ParentOrder po) {
        ParentOrderResponse r = new ParentOrderResponse();
        r.setParentOrderId(po.getId());
        r.setOrderNumber(po.getOrderNumber());
        r.setDeliveryType(po.getDeliveryType());
        r.setDeliveryAddress(po.getDeliveryAddress());
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
    public CustomerInvoiceResponse getCustomerInvoice(Long userId, Long parentOrderId) {
        ParentOrder parentOrder = parentOrderRepository.findById(parentOrderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));

        if (!parentOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        // Build one section per sub-order (shop)
        List<CustomerInvoiceResponse.ShopInvoiceSection> shopSections = parentOrder.getSubOrders()
                .stream().map(subOrder -> {
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
                    return section;
                }).toList();

        CustomerInvoiceResponse r = new CustomerInvoiceResponse();
        r.setOrderNumber(parentOrder.getOrderNumber());
        r.setOrderDate(parentOrder.getCreatedAt());
        r.setCustomerPhone(parentOrder.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(parentOrder.getDeliveryAddress());
        r.setDeliveryType(parentOrder.getDeliveryType());
        r.setShops(shopSections);
        r.setSubtotal(parentOrder.getSubtotal());
        r.setTotalGst(parentOrder.getTotalGst());
        r.setDeliveryCharge(0.0);
        r.setTotalAmount(parentOrder.getTotalAmount());
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
