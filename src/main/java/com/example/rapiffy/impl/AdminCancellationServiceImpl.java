package com.example.rapiffy.impl;

import com.example.rapiffy.dto.admin.cancellation.AdminReviewCancellationRequest;
import com.example.rapiffy.dto.customer.cancellation.CancellationItemResponse;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;
import com.example.rapiffy.enums.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.model.payment.Payment;
import com.example.rapiffy.model.payment.Refund;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.repos.payment.PaymentRepository;
import com.example.rapiffy.repos.payment.RefundRepository;
import com.example.rapiffy.services.AdminCancellationService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCancellationServiceImpl implements AdminCancellationService {

    private final CancellationRequestRepository cancellationRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final CancellationItemRepository cancellationItemRepository;
    private final RazorpayClient razorpayClient;

    // ─── GET ALL CANCELLATION REQUESTS FOR SHOP ──────────────────────────────

    @Override
    public List<CancellationRequestResponse> getCancellations(Long shopId) {
        return cancellationRequestRepository.findByOrderShopIdOrderByCreatedAtDesc(shopId)
                .stream().map(this::toResponse).toList();
    }

    // ─── APPROVE OR REJECT ───────────────────────────────────────────────────

    @Override
    @Transactional
    public CancellationRequestResponse reviewCancellation(Long shopId, Long cancellationRequestId, AdminReviewCancellationRequest request) {
        CancellationRequest cancellationRequest = cancellationRequestRepository.findById(cancellationRequestId)
                .orElseThrow(() -> new ApiException("Cancellation request not found", HttpStatus.NOT_FOUND));

        if (!cancellationRequest.getOrder().getShop().getId().equals(shopId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (cancellationRequest.getStatus() != CancellationStatus.REQUESTED)
            throw new ApiException("Cancellation request is already " + cancellationRequest.getStatus(), HttpStatus.BAD_REQUEST);

        cancellationRequest.setAdminNote(request.getAdminNote());

        if (Boolean.TRUE.equals(request.getApproved())) {
            cancellationRequest.setStatus(CancellationStatus.APPROVED);
            applyItemCancellations(cancellationRequest);
            processRefund(cancellationRequest);
        } else {
            if (request.getAdminNote() == null || request.getAdminNote().isBlank())
                throw new ApiException("Admin note is required when rejecting a cancellation", HttpStatus.BAD_REQUEST);
            cancellationRequest.setStatus(CancellationStatus.REJECTED);
        }

        cancellationRequestRepository.save(cancellationRequest);
        return toResponse(cancellationRequest);
    }

    // ─── APPLY ITEM CANCELLATIONS TO THE ORDER ───────────────────────────────

    private void applyItemCancellations(CancellationRequest cancellationRequest) {
        Order subOrder = cancellationRequest.getOrder();

        for (CancellationItem ci : cancellationRequest.getItems()) {
            OrderItem item = ci.getOrderItem();
            if (ci.getQuantityToCancel().equals(item.getQuantity())) {
                // Nullify FK on CancellationItem before deleting OrderItem to avoid FK constraint violation
                ci.setOrderItem(null);
                cancellationItemRepository.save(ci);
                orderItemRepository.delete(item);
            } else {
                item.setQuantity(item.getQuantity() - ci.getQuantityToCancel());
                item.setLineTotal(Math.round(item.getSellingPrice() * item.getQuantity() * 100.0) / 100.0);
                orderItemRepository.save(item);
            }
        }

        // Recalculate sub-order totals
        List<OrderItem> remaining = orderItemRepository.findByOrderId(subOrder.getId());
        if (remaining.isEmpty()) {
            subOrder.setStatus(OrderStatus.CANCELLED);
            subOrder.setCancelledBy(CancelledBy.CUSTOMER);
            subOrder.setCancellationReason(cancellationRequest.getReason());
            subOrder.setCancelledAt(LocalDateTime.now());
            subOrder.setSubtotal(0.0);
            subOrder.setTotalGst(0.0);
            subOrder.setTotalAmount(0.0);
        } else {
            double newSubtotal = remaining.stream().mapToDouble(i -> i.getSellingPrice() * i.getQuantity()).sum();
            double newGst = remaining.stream().mapToDouble(i -> i.getGstAmount() != null ? i.getGstAmount() : 0.0).sum();
            subOrder.setSubtotal(Math.round(newSubtotal * 100.0) / 100.0);
            subOrder.setTotalGst(Math.round(newGst * 100.0) / 100.0);
            subOrder.setTotalAmount(Math.round((newSubtotal + newGst) * 100.0) / 100.0);
        }
        orderRepository.save(subOrder);
    }

    // ─── REFUND BASED ON PAYMENT METHOD ─────────────────────────────────────

    private void processRefund(CancellationRequest cancellationRequest) {
        Order subOrder = cancellationRequest.getOrder();
        PaymentMethod paymentMethod = subOrder.getParentOrder().getPaymentMethod();
        double refundAmount = cancellationRequest.getRefundAmount();

        if (paymentMethod == PaymentMethod.COD) {
            log.info("COD cancellation approved — no refund needed | order {}", subOrder.getOrderNumber());
            return;
        }

        if (paymentMethod == PaymentMethod.WALLET) {
            creditWallet(cancellationRequest);
            return;
        }

        // Online (UPI/Card/Netbanking)
        initiateRazorpayRefund(cancellationRequest);
    }

    private void creditWallet(CancellationRequest cancellationRequest) {
        User customer = cancellationRequest.getCustomer();
        double refundAmount = cancellationRequest.getRefundAmount();

        Wallet wallet = walletRepository.findByUserId(customer.getId()).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(customer);
            w.setBalance(0.0);
            return w;
        });
        wallet.setBalance(Math.round((wallet.getBalance() + refundAmount) * 100.0) / 100.0);
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setUser(customer);
        txn.setType(WalletTransactionType.CREDIT);
        txn.setSource("ORDER_CANCELLATION");
        txn.setAmount(refundAmount);
        txn.setReferenceId(cancellationRequest.getId());
        txn.setNote("₹" + refundAmount + " refunded to wallet for cancellation request #" + cancellationRequest.getId()
                + " | Order: " + cancellationRequest.getOrder().getOrderNumber());
        walletTransactionRepository.save(txn);

        log.info("Wallet refund ₹{} credited for cancellation #{} | order {}",
                refundAmount, cancellationRequest.getId(), cancellationRequest.getOrder().getOrderNumber());
    }

    private void initiateRazorpayRefund(CancellationRequest cancellationRequest) {
        Order subOrder = cancellationRequest.getOrder();
        double refundAmount = cancellationRequest.getRefundAmount();

        Payment payment = paymentRepository.findByParentOrderId(subOrder.getParentOrder().getId())
                .orElseThrow(() -> new ApiException("Payment not found for this order", HttpStatus.NOT_FOUND));

        try {
            JSONObject refundReq = new JSONObject();
            refundReq.put("amount", Math.round(refundAmount * 100)); // paise
            refundReq.put("notes", new JSONObject()
                    .put("reason", cancellationRequest.getReason())
                    .put("cancellation_request_id", cancellationRequest.getId())
                    .put("sub_order", subOrder.getOrderNumber()));

            com.razorpay.Refund rzpRefund = razorpayClient.payments
                    .refund(payment.getRazorpayPaymentId(), refundReq);

            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setOrder(subOrder);
            refund.setRazorpayRefundId(rzpRefund.get("id"));
            refund.setAmount(refundAmount);
            refund.setCancelledBy(CancelledBy.CUSTOMER);
            refund.setReason(cancellationRequest.getReason());
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);

            payment.setRefundedAmount(payment.getRefundedAmount() + refundAmount);
            paymentRepository.save(payment);

            log.info("Razorpay refund initiated ₹{} for cancellation #{} | order {}",
                    refundAmount, cancellationRequest.getId(), subOrder.getOrderNumber());

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for cancellation #{}: {}", cancellationRequest.getId(), e.getMessage());
            throw new ApiException("Refund initiation failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── RESPONSE MAPPER ─────────────────────────────────────────────────────

    public CancellationRequestResponse toResponse(CancellationRequest cr) {
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
    }
}
