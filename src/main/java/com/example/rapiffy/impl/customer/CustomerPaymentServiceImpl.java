package com.example.rapiffy.impl.customer;

import com.example.rapiffy.config.RazorpayConfig;
import com.example.rapiffy.dto.customer.payment.*;
import com.example.rapiffy.enums.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.model.payment.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.repos.payment.*;
import com.example.rapiffy.services.customer.CustomerPaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerPaymentServiceImpl implements CustomerPaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentRepository paymentRepository;
    private final PaymentTransferRepository paymentTransferRepository;
    private final RefundRepository refundRepository;
    private final ParentOrderRepository parentOrderRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PlatformConfigRepository platformConfigRepository;
    private final PlatformCommissionRepository platformCommissionRepository;

    // ─── 1. INITIATE PAYMENT ─────────────────────────────────────────────────

    @Override
    @Transactional
    public InitiatePaymentResponse initiatePayment(Long userId, InitiatePaymentRequest request) {
        ParentOrder parentOrder = getParentOrder(request.getParentOrderId(), userId);

        // Validate: order must be in PAYMENT_PENDING state
        if (parentOrder.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new ApiException("Payment already initiated or order not in payment pending state", HttpStatus.BAD_REQUEST);
        }

        // Check if payment record already exists (customer retrying)
        Payment existingPayment = paymentRepository.findByParentOrderId(parentOrder.getId()).orElse(null);
        if (existingPayment != null && existingPayment.getStatus() == PaymentStatus.PENDING) {
            // Return existing Razorpay order (customer can retry with same order)
            return buildInitiateResponse(existingPayment, parentOrder);
        }

        // Create Razorpay order
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", Math.round(parentOrder.getTotalAmount() * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", parentOrder.getOrderNumber());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Save Payment record in our DB
            User customer = userRepository.findById(userId).orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

            Payment payment = new Payment();
            payment.setParentOrder(parentOrder);
            payment.setCustomer(customer);
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setAmount(parentOrder.getTotalAmount());
            payment.setCurrency("INR");
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            return buildInitiateResponse(payment, parentOrder);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new ApiException("Payment initiation failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── 2. VERIFY PAYMENT ───────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentStatusResponse verifyPayment(Long userId, VerifyPaymentRequest request) {
        // Find our payment record by Razorpay order ID
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId()).orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));

        // Verify this payment belongs to the logged-in user
        if (!payment.getCustomer().getId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }

        // Already verified? Return current status
        if (payment.getStatus() == PaymentStatus.PAID) {
            return buildPaymentStatusResponse(payment);
        }

        // Verify signature: HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, key_secret)
        String generatedSignature = generateSignature(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayConfig.getKeySecret()
        );

        if (!generatedSignature.equals(request.getRazorpaySignature())) {
            throw new ApiException("Payment verification failed. Invalid signature.", HttpStatus.BAD_REQUEST);
        }

        // Signature valid — mark payment as PAID
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update ParentOrder
        ParentOrder parentOrder = payment.getParentOrder();
        parentOrder.setPaymentStatus(PaymentStatus.PAID);
        parentOrder.setStatus(OrderStatus.PENDING);
        parentOrderRepository.save(parentOrder);

        // Update all sub-orders: PAYMENT_PENDING → PENDING (visible to admins now)
        for (com.example.rapiffy.model.Order subOrder : parentOrder.getSubOrders()) {
            if (subOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
                subOrder.setStatus(OrderStatus.PENDING);
                orderRepository.save(subOrder);
            }
        }

        // Create transfers to each shop (split payment)
        createTransfersForShops(payment, parentOrder);

        return buildPaymentStatusResponse(payment);
    }

    // ─── 3. GET PAYMENT STATUS ───────────────────────────────────────────────

    @Override
    public PaymentStatusResponse getPaymentStatus(Long userId, Long parentOrderId) {
        ParentOrder parentOrder = getParentOrder(parentOrderId, userId);

        Payment payment = paymentRepository.findByParentOrderId(parentOrder.getId()).orElseThrow(() -> new ApiException("Payment not found for this order", HttpStatus.NOT_FOUND));

        return buildPaymentStatusResponse(payment);
    }

    // ─── 4. CANCEL SUB-ORDER ─────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentStatusResponse cancelSubOrder(Long userId, Long subOrderId, CancelSubOrderRequest request) {
        com.example.rapiffy.model.Order subOrder = orderRepository.findById(subOrderId).orElseThrow(() -> new ApiException("Sub-order not found", HttpStatus.NOT_FOUND));

        // Verify customer owns this order
        if (!subOrder.getCustomer().getId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }

        // Can only cancel if PENDING or PAYMENT_PENDING (admin hasn't confirmed yet)
        if (subOrder.getStatus() != OrderStatus.PENDING && subOrder.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new ApiException("Cannot cancel. Order already " + subOrder.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Mark sub-order as cancelled
        subOrder.setStatus(OrderStatus.CANCELLED);
        subOrder.setCancelledBy(CancelledBy.CUSTOMER);
        subOrder.setCancellationReason(request.getReason());
        subOrder.setCancelledAt(LocalDateTime.now());
        orderRepository.save(subOrder);

        // Find payment for parent order
        ParentOrder parentOrder = subOrder.getParentOrder();
        Payment payment = paymentRepository.findByParentOrderId(parentOrder.getId()).orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));

        // Initiate refund for this sub-order's amount
        initiateRefund(payment, subOrder);

        // Reverse transfer if one was created
        reverseTransferIfExists(subOrder);

        // Update parent order status/refund amount
        updateParentOrderAfterCancellation(parentOrder, payment);

        return buildPaymentStatusResponse(payment);
    }

    // ─── 5. GET REFUND HISTORY ───────────────────────────────────────────────

    @Override
    public RefundHistoryResponse getRefundHistory(Long userId, Long parentOrderId) {
        ParentOrder parentOrder = getParentOrder(parentOrderId, userId);

        Payment payment = paymentRepository.findByParentOrderId(parentOrder.getId()).orElseThrow(() -> new ApiException("Payment not found", HttpStatus.NOT_FOUND));

        List<Refund> refunds = refundRepository.findByPaymentId(payment.getId());

        RefundHistoryResponse response = new RefundHistoryResponse();
        response.setParentOrderId(parentOrder.getId());
        response.setOrderNumber(parentOrder.getOrderNumber());
        response.setTotalPaid(payment.getAmount());
        response.setTotalRefunded(payment.getRefundedAmount());
        response.setRefunds(refunds.stream().map(this::toRefundResponse).toList());
        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create Razorpay transfers — split payment to each shop.
     * Commission is calculated per-item based on category.
     * Called after payment is verified.
     */
    private void createTransfersForShops(Payment payment, ParentOrder parentOrder) {
        for (com.example.rapiffy.model.Order subOrder : parentOrder.getSubOrders()) {
            // Skip cancelled sub-orders
            if (subOrder.getStatus() == OrderStatus.CANCELLED) continue;

            Profile shop = subOrder.getShop();
            String linkedAccountId = shop.getRazorpayLinkedAccountId();

            // If shop doesn't have a linked account yet, skip transfer (settle manually later)
            if (linkedAccountId == null || linkedAccountId.isBlank()) {
                log.warn("Shop {} has no linked account. Skipping transfer.", shop.getShopName());
                continue;
            }

            // Calculate commission per-item based on category
            double totalCommission = 0.0;
            for (com.example.rapiffy.model.OrderItem item : subOrder.getItems()) {
                Category category = (item.getShopProduct() != null)
                        ? item.getShopProduct().getCategory() : null;
                double rate = getCommissionRateForCategory(category);
                double itemCommission = item.getLineTotal() * (rate / 100);
                totalCommission += itemCommission;
            }

            Double subOrderAmount = subOrder.getTotalAmount();
            Double commission = Math.round(totalCommission * 100.0) / 100.0;
            Double transferAmount = Math.round((subOrderAmount - commission) * 100.0) / 100.0;

            try {
                // Call Razorpay API to create transfer
                JSONObject transferRequest = new JSONObject();
                transferRequest.put("account", linkedAccountId);
                transferRequest.put("amount", Math.round(transferAmount * 100)); // paise
                transferRequest.put("currency", "INR");

                JSONObject notes = new JSONObject();
                notes.put("sub_order", subOrder.getOrderNumber());
                notes.put("shop_name", shop.getShopName());
                transferRequest.put("notes", notes);

                // Create transfer via Razorpay
                JSONObject transfersPayload = new JSONObject();
                transfersPayload.put("transfers", new org.json.JSONArray().put(transferRequest));

                List<com.razorpay.Transfer> transfers = razorpayClient.payments.transfer(payment.getRazorpayPaymentId(), transfersPayload);

                // Get transfer ID from response
                String razorpayTransferId = (transfers != null && !transfers.isEmpty()) ? transfers.get(0).get("id") : null;

                // Save transfer record
                PaymentTransfer transfer = new PaymentTransfer();
                transfer.setPayment(payment);
                transfer.setOrder(subOrder);
                transfer.setShop(shop);
                transfer.setRazorpayLinkedAccountId(linkedAccountId);
                transfer.setRazorpayTransferId(razorpayTransferId);
                transfer.setAmount(transferAmount);
                transfer.setPlatformCommission(commission);
                transfer.setStatus(TransferStatus.CREATED);
                transfer.setTransferredAt(LocalDateTime.now());
                paymentTransferRepository.save(transfer);

                log.info("Transfer created: ₹{} to {} (commission ₹{})",
                        transferAmount, shop.getShopName(), commission);

            } catch (RazorpayException e) {
                log.error("Transfer failed for shop {}: {}", shop.getShopName(), e.getMessage());
                // Save as FAILED — can retry later
                PaymentTransfer transfer = new PaymentTransfer();
                transfer.setPayment(payment);
                transfer.setOrder(subOrder);
                transfer.setShop(shop);
                transfer.setRazorpayLinkedAccountId(linkedAccountId);
                transfer.setAmount(transferAmount);
                transfer.setPlatformCommission(commission);
                transfer.setStatus(TransferStatus.FAILED);
                paymentTransferRepository.save(transfer);
            }
        }
    }

    /**
     * Initiate refund for a cancelled sub-order.
     */
    private void initiateRefund(Payment payment, com.example.rapiffy.model.Order subOrder) {
        Double refundAmount = subOrder.getTotalAmount();

        try {
            // Call Razorpay API to create refund
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", Math.round(refundAmount * 100)); // paise
            refundRequest.put("notes", new JSONObject()
                    .put("reason", subOrder.getCancellationReason())
                    .put("sub_order", subOrder.getOrderNumber()));

            com.razorpay.Refund rzpRefund = razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(), refundRequest);
            String razorpayRefundId = rzpRefund.get("id");

            // Save refund record
            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setOrder(subOrder);
            refund.setRazorpayRefundId(razorpayRefundId);
            refund.setAmount(refundAmount);
            refund.setCancelledBy(CancelledBy.CUSTOMER);
            refund.setReason(subOrder.getCancellationReason());
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);

            // Update payment refunded amount
            payment.setRefundedAmount(payment.getRefundedAmount() + refundAmount);
            paymentRepository.save(payment);

            // Mark sub-order as refunded
            subOrder.setRefunded(true);
            orderRepository.save(subOrder);

            log.info("Refund initiated: ₹{} for sub-order {}", refundAmount, subOrder.getOrderNumber());

        } catch (RazorpayException e) {
            log.error("Refund failed for sub-order {}: {}", subOrder.getOrderNumber(), e.getMessage());

            // Save as INITIATED — webhook or reconciliation will handle later
            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setOrder(subOrder);
            refund.setAmount(refundAmount);
            refund.setCancelledBy(CancelledBy.CUSTOMER);
            refund.setReason(subOrder.getCancellationReason());
            refund.setStatus(RefundStatus.INITIATED);
            refundRepository.save(refund);
        }
    }

    /**
     * Reverse transfer to shop if it was already created.
     */
    private void reverseTransferIfExists(com.example.rapiffy.model.Order subOrder) {
        paymentTransferRepository.findByOrderId(subOrder.getId()).ifPresent(transfer -> {
            if (transfer.getStatus() == TransferStatus.CREATED) {
                try {
                    // Razorpay: reverse the transfer
                    razorpayClient.transfers.reversal(transfer.getRazorpayTransferId(), new JSONObject());
                    transfer.setStatus(TransferStatus.REVERSED);
                    transfer.setReversedAt(LocalDateTime.now());
                    paymentTransferRepository.save(transfer);
                    log.info("Transfer reversed: {} for shop {}", transfer.getRazorpayTransferId(),
                            transfer.getShop().getShopName());
                } catch (RazorpayException e) {
                    log.error("Transfer reversal failed: {}", e.getMessage());
                    // Will need manual intervention — log it
                }
            }
        });
    }

    /**
     * Update parent order status after a sub-order is cancelled.
     */
    private void updateParentOrderAfterCancellation(ParentOrder parentOrder, Payment payment) {
        // Refresh sub-orders
        List<com.example.rapiffy.model.Order> subOrders = parentOrder.getSubOrders();

        boolean allCancelled = subOrders.stream()
                .allMatch(o -> o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REJECTED);

        if (allCancelled) {
            parentOrder.setStatus(OrderStatus.CANCELLED);
            payment.setStatus(PaymentStatus.FULLY_REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        parentOrder.setRefundedAmount(payment.getRefundedAmount());
        parentOrderRepository.save(parentOrder);
        paymentRepository.save(payment);
    }

    /**
     * Get commission rate for a specific category.
     * Looks up PlatformCommission table by category.
     * Falls back to PlatformConfig.defaultCommissionRate if no entry exists.
     *
     * Example:
     *   Grocery → 3% (from platform_commissions table)
     *   Fashion → 10%
     *   Unknown category → 5% (from platform_config default)
     */
    private Double getCommissionRateForCategory(Category category) {
        if (category != null) {
            Optional<PlatformCommission> catCommission =
                    platformCommissionRepository.findByCategoryAndIsActiveTrue(category);
            if (catCommission.isPresent()) {
                return catCommission.get().getCommissionRate();
            }
        }
        // Fallback: use global default
        return platformConfigRepository.findAll().stream()
                .findFirst()
                .map(PlatformConfig::getDefaultCommissionRate)
                .orElse(5.0); // ultimate fallback: 5%
    }

    /**
     * Verify HMAC-SHA256 signature.
     */
    private String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new ApiException("Signature generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get parent order with ownership check.
     */
    private ParentOrder getParentOrder(Long parentOrderId, Long userId) {
        ParentOrder parentOrder = parentOrderRepository.findById(parentOrderId)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
        if (!parentOrder.getCustomer().getId().equals(userId)) {
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        }
        return parentOrder;
    }

    // ─── RESPONSE BUILDERS ───────────────────────────────────────────────────

    private InitiatePaymentResponse buildInitiateResponse(Payment payment, ParentOrder parentOrder) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setRazorpayOrderId(payment.getRazorpayOrderId());
        response.setAmount(Math.round(parentOrder.getTotalAmount() * 100)); // paise
        response.setCurrency("INR");
        response.setRazorpayKeyId(razorpayConfig.getKeyId());
        response.setOrderNumber(parentOrder.getOrderNumber());
        return response;
    }

    private PaymentStatusResponse buildPaymentStatusResponse(Payment payment) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setParentOrderId(payment.getParentOrder().getId());
        response.setOrderNumber(payment.getParentOrder().getOrderNumber());
        response.setPaymentStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setAmount(payment.getAmount());
        response.setRefundedAmount(payment.getRefundedAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaidAt(payment.getPaidAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }

    private RefundResponse toRefundResponse(Refund refund) {
        RefundResponse r = new RefundResponse();
        r.setRefundId(refund.getId());
        r.setSubOrderNumber(refund.getOrder() != null ? refund.getOrder().getOrderNumber() : null);
        r.setShopName(refund.getOrder() != null ? refund.getOrder().getShop().getShopName() : null);
        r.setAmount(refund.getAmount());
        r.setStatus(refund.getStatus());
        r.setCancelledBy(refund.getCancelledBy());
        r.setReason(refund.getReason());
        r.setInitiatedAt(refund.getInitiatedAt());
        r.setCompletedAt(refund.getCompletedAt());
        return r;
    }
}
