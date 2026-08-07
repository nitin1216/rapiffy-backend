package com.example.rapiffy.config;

import com.example.rapiffy.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

/**
 * Helper to call Razorpay REST APIs not yet supported by SDK v1.4.8.
 * Currently used for: POST /v2/accounts/{accountId}/bank_account
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayRestClient {

    private static final String BASE_URL = "https://api.razorpay.com/v2/accounts";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final RazorpayConfig razorpayConfig;
    private final OkHttpClient okHttpClient;

    /**
     * Links a bank account to a Razorpay linked account (Route).
     * API: POST /v2/accounts/{accountId}/bank_account
     */
    public void addBankAccount(String linkedAccountId, String beneficiaryName,
                               String accountNumber, String ifsc) {
        JSONObject body = new JSONObject();
        body.put("ifsc_code", ifsc);
        body.put("beneficiary_name", beneficiaryName);
        body.put("account_number", accountNumber);
        body.put("beneficiary_email", "");
        body.put("beneficiary_mobile", "");
        body.put("beneficiary_city", "");
        body.put("beneficiary_state", "");
        body.put("beneficiary_country", "IN");
        body.put("beneficiary_pin", "");
        body.put("beneficiary_address", "");

        String credentials = Base64.getEncoder().encodeToString(
            (razorpayConfig.getKeyId() + ":" + razorpayConfig.getKeySecret()).getBytes());

        Request request = new Request.Builder()
            .url(BASE_URL + "/" + linkedAccountId + "/bank_account")
            .addHeader("Authorization", "Basic " + credentials)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(JSON, body.toString()))
            .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Razorpay bank account link failed for {}: {}", linkedAccountId, responseBody);
                throw new ApiException("Failed to link bank account: " + responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            log.info("Bank account linked to Razorpay account {}", linkedAccountId);
        } catch (IOException e) {
            throw new ApiException("Razorpay bank account link request failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
