package com.example.rapiffy.impl;

import com.example.rapiffy.dto.catalog.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.AdminCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final ProfileRepository profileRepository;
    private final MasterProductRepository masterProductRepository;
    private final ShopProductRepository shopProductRepository;
    private final CategoryRepository categoryRepository;

    public AdminCatalogServiceImpl(ProfileRepository profileRepository,
                                   MasterProductRepository masterProductRepository,
                                   ShopProductRepository shopProductRepository,
                                   CategoryRepository categoryRepository) {
        this.profileRepository = profileRepository;
        this.masterProductRepository = masterProductRepository;
        this.shopProductRepository = shopProductRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── GET CATALOG ──────────────────────────────────────────────────────────

    @Override
    public List<CatalogProductResponse> getCatalog(Long userId) {
        Profile shop = getShopProfile(userId);

        List<MasterProduct> masterProducts =
            masterProductRepository.findByCategoryInAndIsActiveTrue(shop.getShopCategories());

        List<ShopProduct> shopProducts = shopProductRepository.findByShop(shop);
        Map<Long, ShopProduct> activatedMap = shopProducts.stream()
            .filter(sp -> sp.getMasterProduct() != null)
            .collect(Collectors.toMap(sp -> sp.getMasterProduct().getId(), sp -> sp));

        return masterProducts.stream().map(mp -> {
            CatalogProductResponse response = new CatalogProductResponse();
            response.setMasterProductId(mp.getId());
            response.setProductCode(mp.getProductCode());
            response.setProductName(mp.getProductName());
            response.setBrand(mp.getBrand());
            response.setUnit(mp.getUnit());
            response.setUnitValue(mp.getUnitValue());
            response.setMrp(mp.getMrp());
            response.setImageUrl(mp.getImageUrl());
            response.setShortDescription(mp.getShortDescription());
            response.setCategoryName(mp.getCategory().getCategoryName());

            ShopProduct sp = activatedMap.get(mp.getId());
            if (sp != null && sp.isActive()) {
                response.setActivatedInShop(true);
                response.setShopProductId(sp.getId());
                response.setSellingPrice(sp.getSellingPrice());
                response.setStockQuantity(sp.getStockQuantity());
            } else {
                response.setActivatedInShop(false);
            }

            return response;
        }).collect(Collectors.toList());
    }

    // ── ACTIVATE PRODUCT ─────────────────────────────────────────────────────

    @Override
    public CatalogActionResponse activateProduct(Long userId, ActivateProductRequest request) {
        Profile shop = getShopProfile(userId);

        MasterProduct masterProduct = masterProductRepository.findById(request.getMasterProductId())
            .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        ShopProduct sp;
        var existingOpt = shopProductRepository.findByShopAndMasterProduct(shop, masterProduct);
        if (existingOpt.isPresent()) {
            ShopProduct existing = existingOpt.get();
            if (existing.isActive()) {
                throw new ApiException("Product already activated in your shop", HttpStatus.CONFLICT);
            }
            sp = existing;
        } else {
            sp = new ShopProduct();
            sp.setShop(shop);
            sp.setMasterProduct(masterProduct);
        }
        sp.setSellingPrice(request.getSellingPrice() != null ? request.getSellingPrice() : 0.0);
        sp.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        sp.setThresholdQuantity(request.getThresholdQuantity());
        sp.setExpiryDate(request.getExpiryDate());
        sp.setActive(true);

        // Override fields
        sp.setProductName(request.getProductName() != null ? request.getProductName() : masterProduct.getProductName());
        sp.setShortDescription(request.getShortDescription() != null ? request.getShortDescription() : masterProduct.getShortDescription());
        sp.setLongDescription(request.getLongDescription() != null ? request.getLongDescription() : masterProduct.getLongDescription());
        sp.setBrand(request.getBrand() != null ? request.getBrand() : masterProduct.getBrand());
        sp.setUnit(request.getUnit() != null ? request.getUnit() : masterProduct.getUnit());
        sp.setUnitValue(request.getUnitValue() != null ? request.getUnitValue() : masterProduct.getUnitValue());
        sp.setMrp(request.getMrp() != null ? request.getMrp() : masterProduct.getMrp());
        sp.setImageUrl(request.getImageUrl() != null ? request.getImageUrl() : masterProduct.getImageUrl());

        // Handle variants
        sp.setHasVariants(request.isHasVariants());
        if (request.isHasVariants() && request.getVariants() != null) {
            for (VariantRequest vr : request.getVariants()) {
                ProductVariant variant = buildVariant(vr, sp);
                sp.getVariants().add(variant);
            }
        }

        ShopProduct saved = shopProductRepository.save(sp);
        return new CatalogActionResponse(saved.getId(), "Product activated successfully");
    }

    // ── UPDATE PRODUCT ───────────────────────────────────────────────────────

    @Override
    public CatalogActionResponse updateProduct(Long userId, Long shopProductId, UpdateProductRequest request) {
        Profile shop = getShopProfile(userId);

        ShopProduct sp = shopProductRepository.findByIdAndShop(shopProductId, shop)
            .orElseThrow(() -> new ApiException("Shop product not found", HttpStatus.NOT_FOUND));

        // Update basic fields (only non-null)
        if (request.getProductName() != null) sp.setProductName(request.getProductName());
        if (request.getShortDescription() != null) sp.setShortDescription(request.getShortDescription());
        if (request.getLongDescription() != null) sp.setLongDescription(request.getLongDescription());
        if (request.getBrand() != null) sp.setBrand(request.getBrand());
        if (request.getImageUrl() != null) sp.setImageUrl(request.getImageUrl());
        if (request.getMrp() != null) sp.setMrp(request.getMrp());
        if (request.getSellingPrice() != null) sp.setSellingPrice(request.getSellingPrice());
        if (request.getStockQuantity() != null) sp.setStockQuantity(request.getStockQuantity());
        if (request.getThresholdQuantity() != null) sp.setThresholdQuantity(request.getThresholdQuantity());
        if (request.getUnit() != null) sp.setUnit(request.getUnit());
        if (request.getUnitValue() != null) sp.setUnitValue(request.getUnitValue());
        if (request.getExpiryDate() != null) sp.setExpiryDate(request.getExpiryDate());

        // Handle variants update
        if (request.getHasVariants() != null) {
            sp.setHasVariants(request.getHasVariants());
        }

        if (request.getVariants() != null) {
            syncVariants(sp, request.getVariants());
        }

        shopProductRepository.save(sp);
        return new CatalogActionResponse(sp.getId(), "Product updated successfully");
    }

    // ── SET VISIBILITY ───────────────────────────────────────────────────────

    @Override
    public CatalogActionResponse setProductVisibility(Long userId, Long shopProductId, boolean active) {
        Profile shop = getShopProfile(userId);

        ShopProduct sp = shopProductRepository.findByIdAndShop(shopProductId, shop)
            .orElseThrow(() -> new ApiException("Shop product not found", HttpStatus.NOT_FOUND));

        sp.setActive(active);
        shopProductRepository.save(sp);
        String msg = active ? "Product is now visible to customers" : "Product is now hidden from customers";
        return new CatalogActionResponse(sp.getId(), msg);
    }

    // ── ADD UNLISTED PRODUCT ─────────────────────────────────────────────────

    @Override
    public CatalogActionResponse addUnlistedProduct(Long userId, AddUnlistedProductRequest request) {
        Profile shop = getShopProfile(userId);

        if (!shop.isEditUnlistedProducts()) {
            throw new ApiException("Your shop is not allowed to add unlisted products", HttpStatus.FORBIDDEN);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        boolean hasCategory = shop.getShopCategories().stream()
            .anyMatch(c -> c.getId().equals(category.getId()));
        if (!hasCategory) {
            throw new ApiException("Category not assigned to your shop", HttpStatus.FORBIDDEN);
        }

        ShopProduct sp = new ShopProduct();
        sp.setShop(shop);
        sp.setMasterProduct(null);
        sp.setCategory(category);
        sp.setProductName(request.getProductName());
        sp.setSellingPrice(request.getSellingPrice() != null ? request.getSellingPrice() : 0.0);
        sp.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
        sp.setThresholdQuantity(request.getThresholdQuantity());
        sp.setShortDescription(request.getShortDescription());
        sp.setLongDescription(request.getLongDescription());
        sp.setBrand(request.getBrand());
        sp.setImageUrl(request.getImageUrl());
        sp.setMrp(request.getMrp());
        sp.setUnit(request.getUnit());
        sp.setUnitValue(request.getUnitValue());
        sp.setExpiryDate(request.getExpiryDate());
        sp.setActive(true);

        // Handle variants
        sp.setHasVariants(request.isHasVariants());
        if (request.isHasVariants() && request.getVariants() != null) {
            for (VariantRequest vr : request.getVariants()) {
                ProductVariant variant = buildVariant(vr, sp);
                sp.getVariants().add(variant);
            }
        }

        ShopProduct saved = shopProductRepository.save(sp);
        return new CatalogActionResponse(saved.getId(), "Unlisted product added successfully");
    }

    @Override
    public CategoryProductsResponse getMyProductsByCategory(Long userId, Long categoryId) {
        Profile shop = getShopProfile(userId);
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        boolean belongs = shop.getShopCategories().stream().anyMatch(c -> c.getId().equals(categoryId));
        if (!belongs)
            throw new ApiException("Category not assigned to your shop", HttpStatus.FORBIDDEN);

        List<ShopProduct> products = shopProductRepository.findByShopAndCategory(shop, category);

        List<ShopProductResponse> responses = products.stream().map(sp -> {
            ShopProductResponse r = new ShopProductResponse();
            r.setShopProductId(sp.getId());
            r.setMasterProductId(sp.getMasterProduct() != null ? sp.getMasterProduct().getId() : null);
            r.setProductName(sp.getProductName());
            r.setBrand(sp.getBrand());
            r.setUnit(sp.getUnit());
            r.setUnitValue(sp.getUnitValue());
            r.setMrp(sp.getMrp());
            r.setSellingPrice(sp.getSellingPrice());
            r.setStockQuantity(sp.getStockQuantity());
            r.setThresholdQuantity(sp.getThresholdQuantity());
            r.setImageUrl(sp.getImageUrl());
            r.setShortDescription(sp.getShortDescription());
            r.setExpiryDate(sp.getExpiryDate());
            r.setHasVariants(sp.isHasVariants());
            r.setActive(sp.isActive());
            r.setUnlisted(sp.getMasterProduct() == null);
            r.setCategoryName(category.getCategoryName());
            return r;
        }).collect(Collectors.toList());

        CategoryProductsResponse result = new CategoryProductsResponse();
        result.setCategoryName(category.getCategoryName());
        result.setProducts(responses);
        return result;
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Profile getShopProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Build a new ProductVariant from VariantRequest.
     */
    private ProductVariant buildVariant(VariantRequest vr, ShopProduct sp) {
        ProductVariant variant = new ProductVariant();
        variant.setShopProduct(sp);
        variant.setVariantName(vr.getVariantName());
        variant.setBrand(vr.getBrand());
        variant.setUnit(vr.getUnit());
        variant.setUnitValue(vr.getUnitValue());
        variant.setMrp(vr.getMrp());
        variant.setSellingPrice(vr.getSellingPrice());
        variant.setStockQuantity(vr.getStockQuantity());
        variant.setThresholdQuantity(vr.getThresholdQuantity());
        variant.setImageUrl(vr.getImageUrl());
        variant.setExpiryDate(vr.getExpiryDate());
        variant.setActive(true);
        return variant;
    }

    /**
     * Sync variants:
     * - Has id → update existing
     * - No id → add new
     * - Missing from list → remove
     */
    private void syncVariants(ShopProduct sp, List<VariantRequest> variantRequests) {
        // Map existing variants by id
        Map<Long, ProductVariant> existingMap = sp.getVariants().stream()
            .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        // Track which existing ids are still present in the request
        Set<Long> requestedIds = new HashSet<>();

        List<ProductVariant> updatedList = new ArrayList<>();

        for (VariantRequest vr : variantRequests) {
            if (vr.getId() != null && existingMap.containsKey(vr.getId())) {
                // Update existing variant
                ProductVariant existing = existingMap.get(vr.getId());
                if (vr.getVariantName() != null) existing.setVariantName(vr.getVariantName());
                if (vr.getBrand() != null) existing.setBrand(vr.getBrand());
                if (vr.getUnit() != null) existing.setUnit(vr.getUnit());
                if (vr.getUnitValue() != null) existing.setUnitValue(vr.getUnitValue());
                if (vr.getMrp() != null) existing.setMrp(vr.getMrp());
                if (vr.getSellingPrice() != null) existing.setSellingPrice(vr.getSellingPrice());
                if (vr.getStockQuantity() != null) existing.setStockQuantity(vr.getStockQuantity());
                if (vr.getThresholdQuantity() != null) existing.setThresholdQuantity(vr.getThresholdQuantity());
                if (vr.getImageUrl() != null) existing.setImageUrl(vr.getImageUrl());
                if (vr.getExpiryDate() != null) existing.setExpiryDate(vr.getExpiryDate());
                updatedList.add(existing);
                requestedIds.add(vr.getId());
            } else {
                // Add new variant
                ProductVariant newVariant = buildVariant(vr, sp);
                updatedList.add(newVariant);
            }
        }

        // Remove variants not in the request (orphanRemoval handles DB delete)
        sp.getVariants().clear();
        sp.getVariants().addAll(updatedList);
    }

    // ── MY PRODUCTS ───────────────────────────────────────────────────────────────

    @Override
    public List<CategoryProductsResponse> getMyProducts(Long userId) {
        Profile shop = getShopProfile(userId);
        List<ShopProduct> products = shopProductRepository.findByShop(shop);

        // Group products by category name
        Map<String, List<ShopProductResponse>> grouped = new LinkedHashMap<>();

        for (ShopProduct sp : products) {
            String categoryName = sp.getCategory() != null
                ? sp.getCategory().getCategoryName()
                : (sp.getMasterProduct() != null ? sp.getMasterProduct().getCategory().getCategoryName() : "Uncategorized");

            ShopProductResponse r = new ShopProductResponse();
            r.setShopProductId(sp.getId());
            r.setMasterProductId(sp.getMasterProduct() != null ? sp.getMasterProduct().getId() : null);
            r.setProductName(sp.getProductName());
            r.setBrand(sp.getBrand());
            r.setUnit(sp.getUnit());
            r.setUnitValue(sp.getUnitValue());
            r.setMrp(sp.getMrp());
            r.setSellingPrice(sp.getSellingPrice());
            r.setStockQuantity(sp.getStockQuantity());
            r.setThresholdQuantity(sp.getThresholdQuantity());
            r.setImageUrl(sp.getImageUrl());
            r.setShortDescription(sp.getShortDescription());
            r.setExpiryDate(sp.getExpiryDate());
            r.setHasVariants(sp.isHasVariants());
            r.setActive(sp.isActive());
            r.setUnlisted(sp.getMasterProduct() == null);
            r.setCategoryName(categoryName);

            grouped.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(r);
        }

        return grouped.entrySet().stream().map(entry -> {
            CategoryProductsResponse cat = new CategoryProductsResponse();
            cat.setCategoryName(entry.getKey());
            cat.setProducts(entry.getValue());
            return cat;
        }).collect(Collectors.toList());
    }
}
