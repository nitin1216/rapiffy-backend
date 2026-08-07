package com.example.rapiffy.scheduler;

import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.enums.PaymentMethod;
import com.example.rapiffy.enums.PaymentStatus;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.ParentOrder;
import com.example.rapiffy.model.payment.Payment;
import com.example.rapiffy.repos.OrderRepository;
import com.example.rapiffy.repos.ParentOrderRepository;
import com.example.rapiffy.repos.payment.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job that reconciles payment status with Razorpay.
 *
 * Purpose: Safety net for cases where both frontend callback and webhook fail.
 * Runs every 15 minutes, checks for PENDING payments older than 10 minutes,
 * and asks Razorpay directly for the real status.
 *
 * Scenarios handled:
 * - Customer paid but frontend crashed before calling /verify
 * - Webhook was down and missed the event
 * - Network glitch during verification
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final ParentOrderRepository parentOrderRepository;
    private final OrderRepository orderRepository;
    private final RazorpayClient razorpayClient;

    /**
     * Runs every 15 minutes.
     * Finds payments stuck in PENDING for more than 10 minutes and syncs with Razorpay.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // every 15 minutes
    @Transactional
    public void reconcilePendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Payment> stalePayments = paymentRepository
                .findByStatusAndCreatedAtBefore(PaymentStatus.PENDING, cutoff);

        if (stalePayments.isEmpty()) return;

        log.info("Reconciliation: found {} stale PENDING payments", stalePayments.size());

        for (Payment payment : stalePayments) {
            try {
                reconcilePayment(payment);
            } catch (Exception e) {
                log.error("Reconciliation failed for payment {}: {}",
                        payment.getRazorpayOrderId(), e.getMessage());
            }
        }
    }

    private void reconcilePayment(Payment payment) {
        try {
            // Ask Razorpay: what's the real status of this order?
            com.razorpay.Order rzpOrder = razorpayClient.orders.fetch(payment.getRazorpayOrderId());
            String rzpStatus = rzpOrder.get("status");

            log.info("Reconciliation: order {} → Razorpay status: {}",
                    payment.getRazorpayOrderId(), rzpStatus);

            switch (rzpStatus) {
                case "paid":
                    // Payment was captured but we missed it — update now
                    handlePaidReconciliation(payment, rzpOrder);
                    break;

                case "attempted":
                    // Customer tried but payment failed — check if too old
                    handleAttemptedReconciliation(payment);
                    break;

                case "created":
                    // Order created but customer never opened checkout — expire if too old
                    handleCreatedReconciliation(payment);
                    break;

                default:
                    log.info("Reconciliation: unknown status {} for order {}",
                            rzpStatus, payment.getRazorpayOrderId());
            }

        } catch (RazorpayException e) {
            log.error("Razorpay API error during reconciliation: {}", e.getMessage());
        }
    }

    /**
     * Razorpay says "paid" but our DB says "PENDING" — sync it.
     */
    private void handlePaidReconciliation(Payment payment, com.razorpay.Order rzpOrder)
            throws RazorpayException {

        // Fetch the payment details from Razorpay to get payment_id
        List<com.razorpay.Payment> payments = razorpayClient.orders.fetchPayments(payment.getRazorpayOrderId());
        if (payments == null || payments.isEmpty()) {
            log.warn("No payments found for paid order: {}", payment.getRazorpayOrderId());
            return;
        }

        // Get the captured payment
        com.razorpay.Payment capturedPayment = null;
        for (com.razorpay.Payment p : payments) {
            if ("captured".equals(p.get("status"))) {
                capturedPayment = p;
                break;
            }
        }

        if (capturedPayment == null) {
            log.warn("No captured payment found for order: {}", payment.getRazorpayOrderId());
            return;
        }

        String razorpayPaymentId = capturedPayment.get("id");
        String method = capturedPayment.has("method") ? capturedPayment.get("method") : null;

        // Update Payment
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaymentMethod(mapPaymentMethod(method));
        paymentRepository.save(payment);

        // Update ParentOrder
        ParentOrder parentOrder = payment.getParentOrder();
        parentOrder.setPaymentStatus(PaymentStatus.PAID);
        parentOrder.setStatus(OrderStatus.PENDING);
        parentOrderRepository.save(parentOrder);

        // Update sub-orders
        for (Order subOrder : parentOrder.getSubOrders()) {
            if (subOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
                subOrder.setStatus(OrderStatus.PENDING);
                orderRepository.save(subOrder);
            }
        }

        log.info("Reconciliation: payment {} synced as PAID", payment.getRazorpayOrderId());

        // Note: Transfers are NOT created here to avoid complexity.
        // They will be created when admin confirms or via a separate transfer job.
    }

    /**
     * Customer attempted payment but it failed.
     * If older than 30 minutes, mark as FAILED.
     */
    private void handleAttemptedReconciliation(Payment payment) {
        if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Reconciliation: payment {} marked FAILED (attempted but expired)",
                    payment.getRazorpayOrderId());
        }
    }

    /**
     * Customer never opened checkout.
     * If older than 60 minutes, mark as FAILED and cancel orders.
     */
    private void handleCreatedReconciliation(Payment payment) {
        if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(60))) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // Cancel the parent order and sub-orders
            ParentOrder parentOrder = payment.getParentOrder();
            parentOrder.setStatus(OrderStatus.CANCELLED);
            parentOrderRepository.save(parentOrder);

            for (Order subOrder : parentOrder.getSubOrders()) {
                if (subOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    subOrder.setStatus(OrderStatus.CANCELLED);
                    subOrder.setCancelledBy(com.example.rapiffy.enums.CancelledBy.SYSTEM);
                    subOrder.setCancellationReason("Payment not completed within 60 minutes");
                    subOrder.setCancelledAt(LocalDateTime.now());
                    orderRepository.save(subOrder);
                }
            }

            log.info("Reconciliation: payment {} expired — orders cancelled",
                    payment.getRazorpayOrderId());
        }
    }

    private PaymentMethod mapPaymentMethod(String method) {
        if (method == null) return null;
        switch (method.toLowerCase()) {
            case "upi": return PaymentMethod.UPI;
            case "card": return PaymentMethod.CARD;
            case "netbanking": return PaymentMethod.NETBANKING;
            case "wallet": return PaymentMethod.WALLET;
            default: return null;
        }
    }
}
