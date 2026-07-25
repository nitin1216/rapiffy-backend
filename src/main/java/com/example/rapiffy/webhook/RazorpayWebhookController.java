package com.example.rapiffy.webhook;

import com.example.rapiffy.config.RazorpayConfig;
import com.example.rapiffy.webhook.handler.PaymentWebhookHandler;
import com.example.rapiffy.webhook.handler.RefundWebhookHandler;
import com.example.rapiffy.webhook.handler.TransferWebhookHandler;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Razorpay Webhook Controller — receives async notifications from Razorpay.
 *
 * Razorpay calls this endpoint when:
 * - Payment is captured (customer paid successfully)
 * - Payment failed
 * - Refund is processed (money reached customer's bank)
 * - Transfer is settled (money reached shop's bank)
 *
 * This is NOT called by our frontend or any user.
 * It's called directly by Razorpay servers.
 *
 * Setup on Razorpay Dashboard:
 *   Settings → Webhooks → Add New
 *   URL: https://your-domain.com/v1/webhook/razorpay
 *   Secret: (same as razorpay.webhook-secret in application.properties)
 *   Events: payment.captured, payment.failed, refund.processed, transfer.settled
 */
@Hidden // Hide from Swagger — not a user-facing API
@RestController
@RequestMapping("/v1/webhook/razorpay")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private final RazorpayConfig razorpayConfig;
    private final PaymentWebhookHandler paymentHandler;
    private final RefundWebhookHandler refundHandler;
    private final TransferWebhookHandler transferHandler;

    /**
     * Main webhook entry point.
     * Razorpay sends a POST with JSON body + signature header.
     *
     * Flow:
     * 1. Verify signature (confirm it's from Razorpay, not a hacker)
     * 2. Parse event type
     * 3. Route to appropriate handler
     * 4. Return 200 OK (Razorpay retries if we return non-2xx)
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        // Step 1: Verify signature
        if (!verifySignature(payload, signature)) {
            log.warn("Webhook signature verification failed. Rejecting.");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        try {
            // Step 2: Parse event type from payload
            // Payload structure: { "event": "payment.captured", "payload": { ... } }
            org.json.JSONObject json = new org.json.JSONObject(payload);
            String event = json.getString("event");

            log.info("Razorpay webhook received: {}", event);

            // Step 3: Route to handler based on event type
            switch (event) {
                case "payment.captured":
                    paymentHandler.handlePaymentCaptured(json);
                    break;

                case "payment.failed":
                    paymentHandler.handlePaymentFailed(json);
                    break;

                case "refund.processed":
                    refundHandler.handleRefundProcessed(json);
                    break;

                case "transfer.settled":
                    transferHandler.handleTransferSettled(json);
                    break;

                default:
                    log.info("Unhandled webhook event: {}", event);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            // Return 200 anyway — we don't want Razorpay to keep retrying on our errors
            // We log the error and handle it manually later
            return ResponseEntity.ok("OK");
        }
    }

    /**
     * Verify that the webhook is genuinely from Razorpay.
     * Uses HMAC-SHA256 with our webhook secret.
     */
    private boolean verifySignature(String payload, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayConfig.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(expectedSignature);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
