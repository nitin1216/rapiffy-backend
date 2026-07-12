package com.example.rapiffy.impl;

import com.example.rapiffy.dto.superadmin.*;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private final CategoryRepository categoryRepository;
    private final MasterProductRepository masterProductRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public SuperAdminServiceImpl(CategoryRepository categoryRepository,
                                 MasterProductRepository masterProductRepository,
                                 UserRepository userRepository,
                                 ProfileRepository profileRepository) {
        this.categoryRepository = categoryRepository;
        this.masterProductRepository = masterProductRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ── CATEGORY MANAGEMENT ──────────────────────────────────────────────────

    @Override
    public SuperAdminActionResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setCategoryCode(request.getCategoryCode());
        category.setCategoryName(request.getCategoryName());
        category.setImageUrl(request.getImageUrl());
        category.setDescription(request.getDescription());
        category.setActive(true);

        categoryRepository.save(category);
        return new SuperAdminActionResponse("Category '" + request.getCategoryName() + "' created successfully");
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public SuperAdminActionResponse deactivateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        category.setActive(false);
        categoryRepository.save(category);
        return new SuperAdminActionResponse("Category '" + category.getCategoryName() + "' deactivated");
    }

    // ── CSV IMPORT ───────────────────────────────────────────────────────────

    /**
     * CSV format expected:
     * productCode,productName,brand,unit,unitValue,mrp,shortDescription,longDescription,attributes
     *
     * Example row:
     * GRO-001,Basmati Rice,India Gate,KG,5,500,Premium aged rice,Long grain basmati...,{"organic":true}
     *
     * - First row is header (skipped)
     * - Duplicate productCodes are skipped
     * - Empty/invalid rows are skipped
     */
    @Override
    public CsvImportResponse importCatalog(Long categoryId, MultipartFile file) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        List<MasterProduct> productsToSave = new ArrayList<>();
        int totalRows = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                totalRows++;
                String[] columns = parseCsvLine(line);

                // Validate minimum columns (at least productCode and productName)
                if (columns.length < 2 || columns[0].isBlank() || columns[1].isBlank()) {
                    skipped++;
                    continue;
                }

                String productCode = columns[0].trim();

                // Skip if productCode already exists
                if (masterProductRepository.existsByProductCode(productCode)) {
                    skipped++;
                    continue;
                }

                MasterProduct product = new MasterProduct();
                product.setProductCode(productCode);
                product.setProductName(columns[1].trim());
                product.setCategory(category);
                product.setBrand(getColumn(columns, 2));
                product.setUnit(getColumn(columns, 3));
                product.setUnitValue(getColumn(columns, 4));
                product.setMrp(parseDouble(getColumn(columns, 5)));
                product.setShortDescription(getColumn(columns, 6));
                product.setLongDescription(getColumn(columns, 7));
                product.setActive(true);

                productsToSave.add(product);
            }
        } catch (Exception e) {
            throw new ApiException("Failed to parse CSV: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Bulk save
        masterProductRepository.saveAll(productsToSave);

        int imported = productsToSave.size();
        return new CsvImportResponse(
            totalRows,
            imported,
            skipped,
            "Imported " + imported + " products into '" + category.getCategoryName() + "'"
        );
    }

    // ── UPDATE MASTER PRODUCT (with variants) ────────────────────────────────

    @Override
    public SuperAdminActionResponse updateMasterProduct(Long masterProductId, UpdateMasterProductRequest request) {
        MasterProduct mp = masterProductRepository.findById(masterProductId)
            .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        // Update basic fields (only non-null)
        if (request.getProductName() != null) mp.setProductName(request.getProductName());
        if (request.getBrand() != null) mp.setBrand(request.getBrand());
        if (request.getUnit() != null) mp.setUnit(request.getUnit());
        if (request.getUnitValue() != null) mp.setUnitValue(request.getUnitValue());
        if (request.getMrp() != null) mp.setMrp(request.getMrp());
        if (request.getImageUrl() != null) mp.setImageUrl(request.getImageUrl());
        if (request.getShortDescription() != null) mp.setShortDescription(request.getShortDescription());
        if (request.getLongDescription() != null) mp.setLongDescription(request.getLongDescription());

        // Handle variants
        if (request.getHasVariants() != null) {
            mp.setHasVariants(request.getHasVariants());
        }

        if (request.getVariants() != null) {
            syncMasterVariants(mp, request.getVariants());
        }

        masterProductRepository.save(mp);
        return new SuperAdminActionResponse("Master product '" + mp.getProductName() + "' updated successfully");
    }

    // ── ADMIN ONBOARDING & REMOVAL ──────────────────────────────────────────

    @Override
    public SuperAdminActionResponse onboardAdmin(OnboardAdminRequest request) {
        // Check if phone already registered
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
        }

        // Create User with ADMIN role
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.ADMIN);
        user.setAuthProvider(AuthProvider.NORMAL);
        User savedUser = userRepository.save(user);

        // Fetch categories
        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        if (categories.isEmpty()) {
            throw new ApiException("No valid categories found", HttpStatus.BAD_REQUEST);
        }

        // Create Profile with shop details
        Profile profile = new Profile();
        profile.setUser(savedUser);
        profile.setShopName(request.getShopName());
        profile.setShopCategories(categories);
        profile.setServingRangeInKm(request.getServingRangeInKm());
        profile.setGstNumber(request.getGstNumber());

        // Set bank details
        if (request.getBankAccountNumber() != null) {
            com.example.rapiffy.common.CBank bank = new com.example.rapiffy.common.CBank();
            bank.setNameOnCard(request.getNameOnCard());
            bank.setMerchantType(request.getMerchantType());
            bank.setBankAccountNumber(request.getBankAccountNumber());
            bank.setIfsc(request.getIfsc());
            profile.setBankDetails(bank);
        }

        profileRepository.save(profile);

        return new SuperAdminActionResponse(
            "Admin '" + request.getShopName() + "' onboarded with " + categories.size() + " categories"
        );
    }

    @Override
    public SuperAdminActionResponse removeAdmin(Long adminUserId) {
        User user = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.getRole() != Roles.ADMIN) {
            throw new ApiException("User is not an Admin", HttpStatus.BAD_REQUEST);
        }

        // Delete profile (shop) and user
        profileRepository.findByUserId(adminUserId).ifPresent(profileRepository::delete);
        userRepository.delete(user);

        return new SuperAdminActionResponse("Admin (userId: " + adminUserId + ") removed successfully");
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private String getColumn(String[] columns, int index) {
        if (index < columns.length && !columns[index].isBlank()) {
            return columns[index].trim();
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Simple CSV line parser that handles quoted fields with commas.
     * e.g. "Hello, World",value2 → ["Hello, World", "value2"]
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Sync master product variants:
     * - Has id → update existing
     * - No id → add new
     * - Missing from list → remove
     */
    private void syncMasterVariants(MasterProduct mp, List<MasterVariantRequest> variantRequests) {
        Map<Long, MasterProductVariant> existingMap = mp.getVariants().stream()
            .collect(Collectors.toMap(MasterProductVariant::getId, v -> v));

        List<MasterProductVariant> updatedList = new ArrayList<>();

        for (MasterVariantRequest vr : variantRequests) {
            if (vr.getId() != null && existingMap.containsKey(vr.getId())) {
                // Update existing
                MasterProductVariant existing = existingMap.get(vr.getId());
                if (vr.getVariantName() != null) existing.setVariantName(vr.getVariantName());
                if (vr.getBrand() != null) existing.setBrand(vr.getBrand());
                if (vr.getUnit() != null) existing.setUnit(vr.getUnit());
                if (vr.getUnitValue() != null) existing.setUnitValue(vr.getUnitValue());
                if (vr.getMrp() != null) existing.setMrp(vr.getMrp());
                if (vr.getImageUrl() != null) existing.setImageUrl(vr.getImageUrl());
                updatedList.add(existing);
            } else {
                // Add new
                MasterProductVariant newVariant = new MasterProductVariant();
                newVariant.setMasterProduct(mp);
                newVariant.setVariantName(vr.getVariantName());
                newVariant.setBrand(vr.getBrand());
                newVariant.setUnit(vr.getUnit());
                newVariant.setUnitValue(vr.getUnitValue());
                newVariant.setMrp(vr.getMrp());
                newVariant.setImageUrl(vr.getImageUrl());
                newVariant.setActive(true);
                updatedList.add(newVariant);
            }
        }

        // Replace — orphanRemoval handles DB delete for removed ones
        mp.getVariants().clear();
        mp.getVariants().addAll(updatedList);
    }
}
