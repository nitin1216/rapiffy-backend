package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.payment.*;

public interface CustomerPaymentService {

    /**
     * Initiate payment — creates Razorpay order for the given ParentOrder.
     * Returns orderId + amount + key for frontend to open Razorpay checkout.
     */
    InitiatePaymentResponse initiatePayment(Long userId, InitiatePaymentRequest request);

    /**
     * Verify payment — validates Razorpay signature after customer pays.
     * Marks payment as PAID, updates order statuses, creates transfers to shops.
     */
    PaymentStatusResponse verifyPayment(Long userId, VerifyPaymentRequest request);

    /**
     * Get payment status — returns current payment status for an order.
     * Also syncs with Razorpay if status is still PENDING (safety net).
     */
    PaymentStatusResponse getPaymentStatus(Long userId, Long parentOrderId);

    /**
     * Cancel sub-order — cancels a specific shop's sub-order.
     * Triggers refund for that sub-order's amount back to customer.
     * Only allowed if sub-order is in PENDING status (admin hasn't confirmed yet).
     */
    PaymentStatusResponse cancelSubOrder(Long userId, Long subOrderId, CancelSubOrderRequest request);

    /**
     * Get refund history — returns all refunds for a parent order.
     * Shows which sub-orders were refunded, amounts, and status.
     */
    RefundHistoryResponse getRefundHistory(Long userId, Long parentOrderId);
}
