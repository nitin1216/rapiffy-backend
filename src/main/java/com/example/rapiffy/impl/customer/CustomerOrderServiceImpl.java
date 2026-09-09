package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.dto.customer.cancellation.CancellationItemResponse;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;
import com.example.rapiffy.dto.order.OrderItemResponse;
import com.example.rapiffy.enums.CancellationStatus;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.repos.CustomerAddressRepository;
import com.example.rapiffy.repos.payment.PaymentRepository;
import com.example.rapiffy.repos.payment.PaymentTransferRepository;
import com.example.rapiffy.repos.payment.RefundRepository;
import com.example.rapiffy.services.customer.CustomerCartService;
import com.example.rapiffy.services.customer.CustomerOrderService;
import com.razorpay.RazorpayClient;
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
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RazorpayClient razorpayClient;
    private final CancellationRequestRepository cancellationRequestRepository;
    private final PaymentTransferRepository paymentTransferRepository;
    private final PlatformConfigRepository platformConfigRepository;

    // ── HAVERSINE DISTANCE ───────────────────────────────────────────────────

    private double calculateDistanceKm(String lat1Str, String lng1Str, String lat2Str, String lng2Str) {
        if (lat1Str == null || lng1Str == null || lat2Str == null || lng2Str == null) return 0.0;
        try {
            double lat1 = Double.parseDouble(lat1Str), lng1 = Double.parseDouble(lng1Str);
            double lat2 = Double.parseDouble(lat2Str), lng2 = Double.parseDouble(lng2Str);
            final double R = 6371;
            double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLng / 2) * Math.sin(dLng / 2);
            return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    @Transactional
    public ParentOrderResponse placeOrder(Long userId, PlaceOrderRequest request) {
        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String resolvedLat = null, resolvedLng = null;
        String resolvedAddress = null;
        CustomerAddress addr = customerAddressRepository.findById(request.getDeliveryAddressId())
                .orElseThrow(() -> new ApiException("Delivery address not found", HttpStatus.BAD_REQUEST));
        if (!addr.getCustomer().getId().equals(userId))
            throw new ApiException("Address does not belong to this customer", HttpStatus.FORBIDDEN);
        resolvedAddress = String.join(", ",
                nullSafe(addr.getAddress().getAddressLine1()),
                nullSafe(addr.getAddress().getCity()),
                nullSafe(addr.getAddress().getState()),
                nullSafe(addr.getAddress().getPinCode()));
        resolvedLat = addr.getAddress().getLatitude();
        resolvedLng = addr.getAddress().getLongitude();

        // Build parent order
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timeStr = now.format(DateTimeFormatter.ofPattern("HHmm"));
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ParentOrder parentOrder = new ParentOrder();
        parentOrder.setCustomer(customer);
        parentOrder.setDeliveryAddress(resolvedAddress);
        parentOrder.setDeliveryInstruction(request.getDeliveryInstruction());
        parentOrder.setOrderNumber("PO-" + dateStr + "" + timeStr + "-" + uniqueSuffix);
        parentOrder.setStatus(OrderStatus.PAYMENT_PENDING);
        log.info("Creating order for user: " + parentOrder.getOrderNumber());
        parentOrder.setSubtotal(0.0);
        parentOrder.setTotalGst(0.0);
        parentOrder.setDeliveryCharge(0.0);
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
        double grandDeliveryCharge = 0.0;
        List<Order> subOrders = new ArrayList<>();
        int shopIndex = 1;

        // Load platform config once for delivery rate
        PlatformConfig platformConfig = platformConfigRepository.findAll().stream().findFirst().orElse(null);
        double ratePerKm = platformConfig != null && platformConfig.getDeliveryChargeRatePerKm() != null
                ? platformConfig.getDeliveryChargeRatePerKm() : 5.0;

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

            double subOrderDeliveryCharge = 0.0;
            // Calculate delivery charge: 2 * distance * ratePerKm
            // Free if subtotal >= shop's freeDeliveryAboveAmount
            if (shop.getFreeDeliveryAboveAmount() == null || subTotal < shop.getFreeDeliveryAboveAmount()) {
                if (shop.getAddress() != null) {
                    double distance = calculateDistanceKm(
                            shop.getAddress().getLatitude(), shop.getAddress().getLongitude(),
                            resolvedLat, resolvedLng);
                    subOrderDeliveryCharge = Math.round(2 * distance * ratePerKm * 100.0) / 100.0;
                }
            }

            double subOrderTotal = Math.round((subTotal + subGst + subOrderDeliveryCharge) * 100.0) / 100.0;

            Order subOrder = new Order();
            subOrder.setParentOrder(savedParent);
            subOrder.setCustomer(customer);
            subOrder.setShop(shop);
            subOrder.setOrderNumber(savedParent.getOrderNumber() + "-S" + shopIndex++);
            subOrder.setDeliveryType(com.example.rapiffy.enums.DeliveryType.SELF);
            subOrder.setDeliveryAddress(resolvedAddress);
            subOrder.setDeliveryLatitude(resolvedLat);
            subOrder.setDeliveryLongitude(resolvedLng);
            subOrder.setSubtotal(Math.round(subTotal * 100.0) / 100.0);
            subOrder.setTotalGst(Math.round(subGst * 100.0) / 100.0);
            subOrder.setDeliveryCharge(subOrderDeliveryCharge);
            subOrder.setTotalAmount(subOrderTotal);
            subOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            Order savedSubOrder = orderRepository.save(subOrder);

            orderItems.forEach(item -> item.setOrder(savedSubOrder));
            orderItemRepository.saveAll(orderItems);
            savedSubOrder.setItems(orderItems);
            subOrders.add(savedSubOrder);

            grandSubtotal += subTotal;
            grandGst += subGst;
            grandDeliveryCharge += subOrderDeliveryCharge;
        }

        // Update parent totals
        savedParent.setSubtotal(Math.round(grandSubtotal * 100.0) / 100.0);
        savedParent.setTotalGst(Math.round(grandGst * 100.0) / 100.0);
        savedParent.setDeliveryCharge(Math.round(grandDeliveryCharge * 100.0) / 100.0);
        savedParent.setTotalAmount(Math.round((grandSubtotal + grandGst + grandDeliveryCharge) * 100.0) / 100.0);
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

    @Override
    public List<OrderItemResponse> getSubOrderItems(Long userId, Long subOrderId) {
        Order subOrder = orderRepository.findById(subOrderId)
                .orElseThrow(() -> new ApiException("Sub-order not found", HttpStatus.NOT_FOUND));

        if (!subOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        return subOrder.getItems().stream().map(item -> {
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
        }).toList();
    }

    // ── CANCEL ORDER ITEMS ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CancelOrderItemResponse cancelOrderItems(Long userId, Long subOrderId, CancelOrderItemRequest request) {
        Order subOrder = orderRepository.findById(subOrderId)
                .orElseThrow(() -> new ApiException("Sub-order not found", HttpStatus.NOT_FOUND));

        if (!subOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (subOrder.getStatus() != OrderStatus.PENDING)
            throw new ApiException("Items can only be cancelled when order status is PENDING", HttpStatus.BAD_REQUEST);

        if (cancellationRequestRepository.existsByOrderIdAndStatus(subOrderId, CancellationStatus.REQUESTED))
            throw new ApiException("A cancellation request is already pending for this order", HttpStatus.BAD_REQUEST);

        User customer = subOrder.getCustomer();
        double totalRefund = 0.0;

        CancellationRequest cancellationRequest = new CancellationRequest();
        cancellationRequest.setOrder(subOrder);
        cancellationRequest.setCustomer(customer);
        cancellationRequest.setReason(request.getReason());
        cancellationRequest.setStatus(CancellationStatus.REQUESTED);

        List<CancellationItem> cancellationItems = new ArrayList<>();
        for (CancelOrderItemRequest.CancelItemEntry entry : request.getItems()) {
            OrderItem item = orderItemRepository.findById(entry.getOrderItemId())
                    .orElseThrow(() -> new ApiException("Order item not found: " + entry.getOrderItemId(), HttpStatus.NOT_FOUND));

            if (!item.getOrder().getId().equals(subOrderId))
                throw new ApiException("Item " + entry.getOrderItemId() + " does not belong to this sub-order", HttpStatus.BAD_REQUEST);

            if (entry.getQuantityToCancel() > item.getQuantity())
                throw new ApiException("Cancel quantity exceeds ordered quantity for: " + item.getProductName(), HttpStatus.BAD_REQUEST);

            double lineRefund = Math.round(item.getSellingPrice() * entry.getQuantityToCancel() * 100.0) / 100.0;
            totalRefund += lineRefund;

            CancellationItem ci = new CancellationItem();
            ci.setCancellationRequest(cancellationRequest);
            ci.setOrderItem(item);
            ci.setProductName(item.getProductName());
            ci.setImageUrl(item.getImageUrl());
            ci.setQuantityToCancel(entry.getQuantityToCancel());
            ci.setLineRefundAmount(lineRefund);
            cancellationItems.add(ci);
        }

        cancellationRequest.setRefundAmount(Math.round(totalRefund * 100.0) / 100.0);
        cancellationRequest.setItems(cancellationItems);
        CancellationRequest saved = cancellationRequestRepository.save(cancellationRequest);

        CancelOrderItemResponse response = new CancelOrderItemResponse();
        response.setCancellationRequestId(saved.getId());
        response.setSubOrderId(subOrder.getId());
        response.setSubOrderNumber(subOrder.getOrderNumber());
        response.setShopName(subOrder.getShop().getShopName());
        response.setSubOrderStatus(subOrder.getStatus());
        response.setCancellationStatus(CancellationStatus.REQUESTED);
        response.setRefundAmount(saved.getRefundAmount());
        response.setMessage("Your cancellation request has been submitted. You will be refunded once the shop approves it.");
        return response;
    }

    // ── GET MY CANCELLATIONS ─────────────────────────────────────────────────

    @Override
    public List<CancellationRequestResponse> getMyCancellations(Long userId) {
        return cancellationRequestRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream().map(cr -> {
                    CancellationRequestResponse r = new CancellationRequestResponse();
                    r.setCancellationRequestId(cr.getId());
                    r.setSubOrderId(cr.getOrder().getId());
                    r.setSubOrderNumber(cr.getOrder().getOrderNumber());
                    r.setShopName(cr.getOrder().getShop().getShopName());
                    r.setReason(cr.getReason());
                    r.setAdminNote(cr.getAdminNote());
                    r.setStatus(cr.getStatus());
                    r.setRefundAmount(cr.getRefundAmount());
                    r.setCreatedAt(cr.getCreatedAt());
                    r.setUpdatedAt(cr.getUpdatedAt());
                    r.setItems(cr.getItems().stream().map(ci -> {
                        CancellationItemResponse item = new CancellationItemResponse();
                        item.setOrderItemId(ci.getOrderItem() != null ? ci.getOrderItem().getId() : null);
                        item.setProductName(ci.getProductName());
                        item.setImageUrl(ci.getImageUrl());
                        item.setQuantityToCancel(ci.getQuantityToCancel());
                        item.setLineRefundAmount(ci.getLineRefundAmount());
                        return item;
                    }).toList());
                    return r;
                }).toList();
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
        r.setDeliveryCharge(po.getDeliveryCharge());
        r.setTotalAmount(po.getTotalAmount());
        r.setCreatedAt(po.getCreatedAt());
        return r;
    }

    private ParentOrderResponse toParentOrderResponse(ParentOrder po) {
        ParentOrderResponse r = new ParentOrderResponse();
        r.setParentOrderId(po.getId());
        r.setOrderNumber(po.getOrderNumber());
        r.setDeliveryAddress(po.getDeliveryAddress());
        r.setDeliveryInstruction(po.getDeliveryInstruction());
        r.setSubtotal(po.getSubtotal());
        r.setTotalGst(po.getTotalGst());
        r.setDeliveryCharge(po.getDeliveryCharge());
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
        section.setShopGstNumber(shop.getGstNumber());
        section.setShopPan(shop.getPan());
        section.setShopState(shop.getAddress() != null ? shop.getAddress().getState() : null);
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

        // Platform fee from PaymentTransfer
        com.example.rapiffy.model.payment.PaymentTransfer transfer =
                paymentTransferRepository.findByOrderId(subOrder.getId()).orElse(null);
        double platformFee = transfer != null ? transfer.getPlatformCommission() : 0.0;
        double platformFeeGst = Math.round(platformFee * 0.18 * 100.0) / 100.0;
        double platformFeeTotal = Math.round((platformFee + platformFeeGst) * 100.0) / 100.0;

        // Place of supply = state from delivery address
        String placeOfSupply = "";
        if (subOrder.getDeliveryAddress() != null && !subOrder.getDeliveryAddress().isBlank()) {
            String[] parts = subOrder.getDeliveryAddress().split(",");
            if (parts.length >= 3) placeOfSupply = parts[parts.length - 2].trim();
        }
        if (placeOfSupply.isBlank() && shop.getAddress() != null)
            placeOfSupply = nullSafe(shop.getAddress().getState());

        CustomerInvoiceResponse r = new CustomerInvoiceResponse();
        r.setOrderNumber(subOrder.getOrderNumber());
        r.setInvoiceNumber(subOrder.getInvoiceId());
        r.setOrderDate(parentOrder.getCreatedAt());
        r.setInvoiceDate(parentOrder.getCreatedAt());
        r.setCustomerPhone(parentOrder.getCustomer().getPhoneNumber());
        r.setDeliveryAddress(subOrder.getDeliveryAddress());
        r.setDeliveryType(subOrder.getDeliveryType());
        r.setPlaceOfSupply(placeOfSupply);
        r.setPlaceOfDelivery(placeOfSupply);
        r.setPlatformFee(platformFee);
        r.setPlatformFeeGst(platformFeeGst);
        r.setPlatformFeeTotal(platformFeeTotal);
        r.setTxnId(transfer != null ? transfer.getRazorpayTransferId() : null);
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
        r.setDeliveryCharge(order.getDeliveryCharge());
        r.setTotalAmount(order.getTotalAmount());
        r.setDeliveryType(order.getDeliveryType());
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
