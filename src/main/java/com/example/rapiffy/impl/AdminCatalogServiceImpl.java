package com.example.rapiffy.impl;

import com.example.rapiffy.dto.catalog.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.AdminCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminCatalogServiceImpl implements AdminCatalogService {

    private final ProfileRepository profileRepository;
    private final ShopProductRepository shopProductRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ProductVariantRepository productVariantRepository;

    public AdminCatalogServiceImpl(ProfileRepository profileRepository,
                                   ShopProductRepository shopProductRepository,
                                   SubCategoryRepository subCategoryRepository,
                                   ProductVariantRepository productVariantRepository) {
        this.profileRepository = profileRepository;
        this.shopProductRepository = shopProductRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productVariantRepository = productVariantRepository;
    }

    // ── MY PRODUCTS (tree: Category → SubCategory → Products) ────────────────

    @Override
    public List<CategoryProductsResponse> getMyProducts(Long userId) {
        Profile shop = getShopProfile(userId);
        List<ShopProduct> products = shopProductRepository.findByShopIdAndIsActive(shop.getId(), true);
        return buildTree(products);
    }

    @Override
    public CategoryProductsResponse getMyProductsBySubCategory(Long userId, Long subCategoryId) {
        Profile shop = getShopProfile(userId);
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));

        boolean belongs = shop.getShopCategories().stream()
            .anyMatch(c -> c.getId().equals(subCategory.getCategory().getId()));
        if (!belongs)
            throw new ApiException("Category not assigned to your shop", HttpStatus.FORBIDDEN);

        List<ShopProduct> products = shopProductRepository.findByShopAndSubCategory(shop, subCategory);

        CategoryProductsResponse result = new CategoryProductsResponse();
        result.setCategoryId(subCategory.getCategory().getId());
        result.setCategoryName(subCategory.getCategory().getCategoryName());

        CategoryProductsResponse.SubCategoryProductsResponse sub = new CategoryProductsResponse.SubCategoryProductsResponse();
        sub.setSubCategoryId(subCategory.getId());
        sub.setSubCategoryName(subCategory.getName());
        sub.setProducts(products.stream().map(this::toShopProductResponse).collect(Collectors.toList()));

        result.setSubCategories(List.of(sub));
        return result;
    }

    // ── UPDATE PRODUCT ───────────────────────────────────────────────────────

    @Override
    public CatalogActionResponse updateProduct(Long userId, Long shopProductId, UpdateProductRequest request) {
        Profile shop = getShopProfile(userId);

        ShopProduct sp = shopProductRepository.findByIdAndShop(shopProductId, shop)
            .orElseThrow(() -> new ApiException("Shop product not found", HttpStatus.NOT_FOUND));

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
        if (request.getHasVariants() != null) sp.setHasVariants(request.getHasVariants());

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

    // ── ADD VARIANTS ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public VariantActionResponse addVariants(Long userId, AddVariantsRequest request) {
        Profile shop = getShopProfile(userId);

        ShopProduct parent = shopProductRepository.findByIdAndShop(request.getParentShopProductId(), shop)
                .orElseThrow(() -> new ApiException("Parent product not found", HttpStatus.NOT_FOUND));

        if (!parent.isHasVariants())
            throw new ApiException("Product does not have variants enabled. Set hasVariants=true first.", HttpStatus.BAD_REQUEST);

        List<ProductVariant> saved = new ArrayList<>();
        for (VariantRequest vr : request.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setParentShopProduct(parent);
            variant.setVariantName(vr.getVariantName());
            variant.setBrand(vr.getBrand());
            variant.setUnit(vr.getUnit());
            variant.setUnitValue(vr.getUnitValue());
            variant.setShortDescription(vr.getShortDescription());
            variant.setLongDescription(vr.getLongDescription());
            variant.setMrp(vr.getMrp());
            variant.setSellingPrice(vr.getSellingPrice() != null ? vr.getSellingPrice() : 0.0);
            variant.setStockQuantity(vr.getStockQuantity() != null ? vr.getStockQuantity() : 0);
            variant.setThresholdQuantity(vr.getThresholdQuantity());
            variant.setImageUrl(vr.getImageUrl());
            variant.setExpiryDate(vr.getExpiryDate());
            variant.setGstSlab(vr.getGstSlab());
            variant.setActive(true);
            saved.add(productVariantRepository.save(variant));
        }

        // Auto-set shopProductId = variant's generated id (reuse PK as shopProductId)
        for (ProductVariant v : saved) {
            v.setShopProductId(v.getId());
        }
        productVariantRepository.saveAll(saved);

        List<VariantActionResponse.VariantInfo> infos = saved.stream()
                .map(v -> new VariantActionResponse.VariantInfo(v.getId(), v.getShopProductId(), v.getVariantName()))
                .collect(Collectors.toList());

        return new VariantActionResponse(parent.getId(), "Variants added successfully", infos);
    }

    // ── ADD UNLISTED PRODUCT ─────────────────────────────────────────────────

    @Override
    public CatalogActionResponse addUnlistedProduct(Long userId, AddUnlistedProductRequest request) {
        Profile shop = getShopProfile(userId);

        if (!shop.isEditUnlistedProducts()) {
            throw new ApiException("Your shop is not allowed to add unlisted products", HttpStatus.FORBIDDEN);
        }

        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));

        boolean hasCategory = shop.getShopCategories().stream()
            .anyMatch(c -> c.getId().equals(subCategory.getCategory().getId()));
        if (!hasCategory)
            throw new ApiException("Category not assigned to your shop", HttpStatus.FORBIDDEN);

        ShopProduct sp = new ShopProduct();
        sp.setShop(shop);
        sp.setMasterProduct(null);
        sp.setSubCategory(subCategory);
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

        sp.setHasVariants(request.isHasVariants());

        ShopProduct saved = shopProductRepository.save(sp);
        return new CatalogActionResponse(saved.getId(), "Unlisted product added successfully");
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private Profile getShopProfile(Long userId) {
        return profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
    }

    private List<CategoryProductsResponse> buildTree(List<ShopProduct> products) {
        Map<String, Map<String, List<ShopProduct>>> tree = new LinkedHashMap<>();

        for (ShopProduct sp : products) {
            SubCategory sc = sp.getSubCategory();
            if (sc == null) continue;
            tree.computeIfAbsent(sc.getCategory().getCategoryName(), k -> new LinkedHashMap<>())
                .computeIfAbsent(sc.getName(), k -> new ArrayList<>())
                .add(sp);
        }

        return tree.entrySet().stream().map(catEntry -> {
            ShopProduct first = catEntry.getValue().values().iterator().next().get(0);
            Category category = first.getSubCategory().getCategory();

            CategoryProductsResponse cat = new CategoryProductsResponse();
            cat.setCategoryId(category.getId());
            cat.setCategoryName(category.getCategoryName());
            cat.setSubCategories(catEntry.getValue().entrySet().stream().map(subEntry -> {
                ShopProduct subFirst = subEntry.getValue().get(0);
                CategoryProductsResponse.SubCategoryProductsResponse sub = new CategoryProductsResponse.SubCategoryProductsResponse();
                sub.setSubCategoryId(subFirst.getSubCategory().getId());
                sub.setSubCategoryName(subFirst.getSubCategory().getName());
                sub.setProducts(subEntry.getValue().stream().map(this::toShopProductResponse).collect(Collectors.toList()));
                return sub;
            }).collect(Collectors.toList()));
            return cat;
        }).collect(Collectors.toList());
    }

    private ShopProductResponse toShopProductResponse(ShopProduct sp) {
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
        if (sp.isHasVariants()) {
            List<VariantResponse> variants = productVariantRepository.findByParentShopProductId(sp.getId())
                .stream().map(v -> {
                    VariantResponse vr = new VariantResponse();
                    vr.setId(v.getId());
                    vr.setVariantName(v.getVariantName());
                    vr.setBrand(v.getBrand());
                    vr.setUnit(v.getUnit());
                    vr.setUnitValue(v.getUnitValue());
                    vr.setMrp(v.getMrp());
                    vr.setSellingPrice(v.getSellingPrice());
                    vr.setStockQuantity(v.getStockQuantity());
                    vr.setThresholdQuantity(v.getThresholdQuantity());
                    vr.setImageUrl(v.getImageUrl());
                    vr.setExpiryDate(v.getExpiryDate());
                    vr.setActive(v.isActive());
                    return vr;
                }).collect(Collectors.toList());
            r.setVariants(variants);
        }
        return r;
    }

    // ── UPDATE VARIANT ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CatalogActionResponse updateVariant(Long userId, Long variantId, VariantRequest request) {
        Profile shop = getShopProfile(userId);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ApiException("Variant not found", HttpStatus.NOT_FOUND));

        // Ensure variant belongs to this shop
        if (!variant.getParentShopProduct().getShop().getId().equals(shop.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getBrand() != null) variant.setBrand(request.getBrand());
        if (request.getUnit() != null) variant.setUnit(request.getUnit());
        if (request.getUnitValue() != null) variant.setUnitValue(request.getUnitValue());
        if (request.getShortDescription() != null) variant.setShortDescription(request.getShortDescription());
        if (request.getLongDescription() != null) variant.setLongDescription(request.getLongDescription());
        if (request.getMrp() != null) variant.setMrp(request.getMrp());
        if (request.getSellingPrice() != null) variant.setSellingPrice(request.getSellingPrice());
        if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
        if (request.getThresholdQuantity() != null) variant.setThresholdQuantity(request.getThresholdQuantity());
        if (request.getImageUrl() != null) variant.setImageUrl(request.getImageUrl());
        if (request.getExpiryDate() != null) variant.setExpiryDate(request.getExpiryDate());
        if (request.getGstSlab() != null) variant.setGstSlab(request.getGstSlab());

        productVariantRepository.save(variant);
        return new CatalogActionResponse(variant.getId(), "Variant updated successfully");
    }

    // ── DELETE VARIANT ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CatalogActionResponse deleteVariant(Long userId, Long variantId) {
        Profile shop = getShopProfile(userId);

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ApiException("Variant not found", HttpStatus.NOT_FOUND));

        if (!variant.getParentShopProduct().getShop().getId().equals(shop.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        productVariantRepository.delete(variant);
        return new CatalogActionResponse(variantId, "Variant deleted successfully");
    }

}

