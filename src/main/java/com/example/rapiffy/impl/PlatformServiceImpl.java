package com.example.rapiffy.impl;

import com.example.rapiffy.config.RazorpayRestClient;
import com.example.rapiffy.dto.platform.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Category;
import com.example.rapiffy.model.PlatformCommission;
import com.example.rapiffy.model.PlatformConfig;
import com.example.rapiffy.repos.CategoryRepository;
import com.example.rapiffy.repos.PlatformCommissionRepository;
import com.example.rapiffy.repos.PlatformConfigRepository;
import com.example.rapiffy.services.PlatformService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformServiceImpl implements PlatformService {

    private final PlatformConfigRepository platformConfigRepository;
    private final PlatformCommissionRepository platformCommissionRepository;
    private final CategoryRepository categoryRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayRestClient razorpayRestClient;

    // ── COMMISSION ACCOUNT ───────────────────────────────────────────────────

    /**
     * Creates a Razorpay linked account for the platform and saves it in PlatformConfig.
     * All customerCommission + shopCommission from every order will be routed here.
     * Call this ONCE during platform setup.
     */
    @Override
    public PlatformActionResponse saveCommissionAccount(SaveCommissionAccountRequest request) {
        PlatformConfig config = getOrCreateConfig();

        if (config.getRazorpayCommissionAccountId() != null && !config.getRazorpayCommissionAccountId().isBlank())
            throw new ApiException("Commission account already set: " + config.getRazorpayCommissionAccountId(), HttpStatus.CONFLICT);

        try {
            JSONObject accountRequest = new JSONObject();
            accountRequest.put("email", request.getEmail());
            accountRequest.put("type", "route");
            accountRequest.put("legal_business_name", request.getBusinessName());
            accountRequest.put("business_type", request.getBusinessType() != null ? request.getBusinessType() : "individual");
            accountRequest.put("profile", new JSONObject()
                .put("category", "ecommerce")
                .put("subcategory", "groceries")
                .put("addresses", new JSONObject()
                    .put("registered", new JSONObject()
                        .put("street1", "NA")
                        .put("city", "NA")
                        .put("state", "Maharashtra")
                        .put("postal_code", "400001")
                        .put("country", "IN"))));

            com.razorpay.Account razorpayAccount = razorpayClient.account.create(accountRequest);
            String commissionAccountId = razorpayAccount.get("id");

            // Link bank account via direct REST call (SDK v1.4.8 doesn't support this)
            razorpayRestClient.addBankAccount(commissionAccountId, request.getBeneficiaryName(),
                request.getBankAccountNumber(), request.getIfsc());

            config.setRazorpayCommissionAccountId(commissionAccountId);
            platformConfigRepository.save(config);

            log.info("Platform commission account created: {}", commissionAccountId);
            return new PlatformActionResponse("Platform commission account created and saved: " + commissionAccountId);

        } catch (RazorpayException e) {
            throw new ApiException("Razorpay commission account creation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── PER-CATEGORY COMMISSION RATES ────────────────────────────────────────

    @Override
    public PlatformActionResponse setCommissionRate(SetCommissionRateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        PlatformCommission commission = platformCommissionRepository
            .findByCategoryAndIsActiveTrue(category)
            .orElseGet(PlatformCommission::new);

        commission.setCategory(category);
        commission.setCommissionRate(request.getCommissionRate());
        commission.setShopCommissionRate(request.getShopCommissionRate() != null ? request.getShopCommissionRate() : 0.0);
        commission.setNotes(request.getNotes());
        commission.setActive(true);
        platformCommissionRepository.save(commission);

        return new PlatformActionResponse(
            "Commission rates set for category '" + category.getCategoryName() +
            "': customer=" + request.getCommissionRate() + "%, shop=" + commission.getShopCommissionRate() + "%");
    }

    @Override
    public PlatformActionResponse deactivateCommissionRate(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        PlatformCommission commission = platformCommissionRepository
            .findByCategoryAndIsActiveTrue(category)
            .orElseThrow(() -> new ApiException("No active commission rate found for this category", HttpStatus.NOT_FOUND));

        commission.setActive(false);
        platformCommissionRepository.save(commission);
        return new PlatformActionResponse("Commission rate deactivated for category '" + category.getCategoryName() + "'");
    }

    @Override
    public List<PlatformCommission> getAllCommissionRates() {
        return platformCommissionRepository.findAll();
    }

    // ── DEFAULT COMMISSION RATE ──────────────────────────────────────────────

    @Override
    public PlatformActionResponse updateDefaultCommissionRate(UpdateDefaultCommissionRequest request) {
        PlatformConfig config = getOrCreateConfig();
        config.setDefaultCommissionRate(request.getDefaultCommissionRate());
        platformConfigRepository.save(config);
        return new PlatformActionResponse("Default commission rate updated to " + request.getDefaultCommissionRate() + "%");
    }

    // ── HELPER ───────────────────────────────────────────────────────────────

    private PlatformConfig getOrCreateConfig() {
        return platformConfigRepository.findAll().stream()
            .findFirst()
            .orElseGet(() -> platformConfigRepository.save(new PlatformConfig()));
    }
}
