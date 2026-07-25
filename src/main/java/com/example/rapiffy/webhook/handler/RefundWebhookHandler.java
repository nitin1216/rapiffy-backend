package com.example.rapiffy.webhook.handler;

import com.example.rapiffy.enums.RefundStatus;
import com.example.rapiffy.model.payment.Refund;
import com.example.rapiffy.repos.payment.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles refund-related webhook events from Razorpay.
 *
 * Event:
 * - refund.processed → Refund money has reached customer's bank account
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefundWebhookHandler {

    private final RefundRepository refundRepository;

    /**
     * refund.processed — Refund completed, money is back in customer's account.
     *
     * Actions:
     * 1. Find Refund by razorpayRefundId
     * 2. Update status = COMPLETED, set completedAt
     */
    @Transactional
    public void handleRefundProcessed(JSONObject webhookPayload) {
        JSONObject refundEntity = webhookPayload
                .getJSONObject("payload")
                .getJSONObject("refund")
                .getJSONObject("entity");

        String razorpayRefundId = refundEntity.getString("id");

        Refund refund = refundRepository.findByRazorpayRefundId(razorpayRefundId).orElse(null);
        if (refund == null) {
            log.warn("Refund not found for razorpayRefundId: {}", razorpayRefundId);
            return;
        }

        // Already completed? Skip
        if (refund.getStatus() == RefundStatus.COMPLETED) {
            log.info("Refund already COMPLETED: {}", razorpayRefundId);
            return;
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refundRepository.save(refund);

        log.info("Refund processed: {} → amount ₹{}", razorpayRefundId, refund.getAmount());
    }
}
