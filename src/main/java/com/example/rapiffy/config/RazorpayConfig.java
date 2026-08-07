package com.example.rapiffy.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Razorpay configuration — reads keys from application.properties.
 *
 * To update keys (e.g. switch from test to live):
 *   → Just change values in application.properties (same as Google client-id)
 *
 * Keys:
 *   razorpay.key-id       → Public key (sent to frontend for checkout)
 *   razorpay.key-secret   → Private key (used for server-side API calls)
 *   razorpay.webhook-secret → Used to verify webhook signatures from Razorpay
 */
@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKeySecret() {
        return keySecret;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }
}
