package com.example.rapiffy.webhook.handler;

import com.example.rapiffy.enums.TransferStatus;
import com.example.rapiffy.model.payment.PaymentTransfer;
import com.example.rapiffy.repos.payment.PaymentTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles transfer-related webhook events from Razorpay.
 *
 * Event:
 * - transfer.settled → Money has reached the shop's bank account
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransferWebhookHandler {

    private final PaymentTransferRepository paymentTransferRepository;

    /**
     * transfer.settled — Transfer money reached shop's bank account.
     *
     * Actions:
     * 1. Find PaymentTransfer by razorpayTransferId
     * 2. Update status = SETTLED, set settledAt
     */
    @Transactional
    public void handleTransferSettled(JSONObject webhookPayload) {
        JSONObject transferEntity = webhookPayload
                .getJSONObject("payload")
                .getJSONObject("transfer")
                .getJSONObject("entity");

        String razorpayTransferId = transferEntity.getString("id");

        PaymentTransfer transfer = paymentTransferRepository
                .findByRazorpayTransferId(razorpayTransferId).orElse(null);
        if (transfer == null) {
            log.warn("Transfer not found for razorpayTransferId: {}", razorpayTransferId);
            return;
        }

        // Already settled? Skip
        if (transfer.getStatus() == TransferStatus.SETTLED) {
            log.info("Transfer already SETTLED: {}", razorpayTransferId);
            return;
        }

        transfer.setStatus(TransferStatus.SETTLED);
        transfer.setSettledAt(LocalDateTime.now());
        paymentTransferRepository.save(transfer);

        log.info("Transfer settled: {} → ₹{} to shop {}",
                razorpayTransferId, transfer.getAmount(),
                transfer.getShop().getShopName());
    }
}
