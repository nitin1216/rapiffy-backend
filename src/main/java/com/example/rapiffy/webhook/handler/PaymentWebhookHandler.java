package com.example.rapiffy.webhook.handler;

import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.enums.PaymentMethod;
import com.example.rapiffy.enums.PaymentStatus;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.ParentOrder;
import com.example.rapiffy.model.payment.Payment;
import com.example.rapiffy.repos.OrderRepository;
import com.example.rapiffy.repos.ParentOrderRepository;
import com.example.rapiffy.repos.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles payment-related webhook events from Razorpay.
 *
 * Events:
 * - payment.captured → Customer paid successfully
 * - payment.failed → Payment attempt failed
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookHandler {

    private final PaymentRepository paymentRepository;
    private final ParentOrderRepository parentOrderRepository;
    private final OrderRepository orderRepository;

    /**
     * payment.captured — Customer successfully paid.
     *
     * Actions:
     * 1. Find Payment by razorpayOrderId
     * 2. Update Payment: status = PAID, save paymentId, method
     * 3. Update ParentOrder: paymentStatus = PAID, status = PENDING
     * 4. Update all sub-Orders: status = PENDING (ready for admin to see)
     */
    @Transactional
    public void handlePaymentCaptured(JSONObject webhookPayload) {
        JSONObject paymentEntity = webhookPayload
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId = paymentEntity.getString("order_id");
        String razorpayPaymentId = paymentEntity.getString("id");
        int amountInPaise = paymentEntity.getInt("amount");
        String method = paymentEntity.optString("method", null);

        // Find our Payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for razorpayOrderId: {}", razorpayOrderId);
            return;
        }

        // Already processed? Skip (idempotency)
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment already marked PAID: {}", razorpayOrderId);
            return;
        }

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

        // Update all sub-orders: now visible to admins
        for (Order subOrder : parentOrder.getSubOrders()) {
            if (subOrder.getStatus() == OrderStatus.PAYMENT_PENDING) {
                subOrder.setStatus(OrderStatus.PENDING);
                orderRepository.save(subOrder);
            }
        }

        log.info("Payment captured: {} → ParentOrder {} now PENDING",
                razorpayPaymentId, parentOrder.getOrderNumber());
    }

    /**
     * payment.failed — Payment attempt failed.
     *
     * Actions:
     * 1. Find Payment by razorpayOrderId
     * 2. Update Payment: status = FAILED
     * 3. ParentOrder stays as PAYMENT_PENDING (customer can retry)
     */
    @Transactional
    public void handlePaymentFailed(JSONObject webhookPayload) {
        JSONObject paymentEntity = webhookPayload
                .getJSONObject("payload")
                .getJSONObject("payment")
                .getJSONObject("entity");

        String razorpayOrderId = paymentEntity.getString("order_id");

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for failed event: {}", razorpayOrderId);
            return;
        }

        // Only mark FAILED if still PENDING (not already PAID via another attempt)
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment failed for order: {}", razorpayOrderId);
        }
    }

    private PaymentMethod mapPaymentMethod(String method) {
        if (method == null) return null;
        return switch (method.toLowerCase()) {
            case "upi" -> PaymentMethod.UPI;
            case "card" -> PaymentMethod.CARD;
            case "netbanking" -> PaymentMethod.NETBANKING;
            case "wallet" -> PaymentMethod.WALLET;
            default -> null;
        };
    }
}
