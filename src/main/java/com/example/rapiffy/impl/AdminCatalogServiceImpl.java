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
    private final VariantAttributeTypeRepository variantAttributeTypeRepository;
    private final VariantAttributeValueRepository variantAttributeValueRepository;
    private final ShopProductImageRepository shopProductImageRepository;
    private final ProductVariantImageRepository productVariantImageRepository;

    public AdminCatalogServiceImpl(ProfileRepository profileRepository,
                                   ShopProductRepository shopProductRepository,
                                   SubCategoryRepository subCategoryRepository,
                                   ProductVariantRepository productVariantRepository,
                                   VariantAttributeTypeRepository variantAttributeTypeRepository,
                                   VariantAttributeValueRepository variantAttributeValueRepository,
                                   ShopProductImageRepository shopProductImageRepository,
                                   ProductVariantImageRepository productVariantImageRepository) {
        this.profileRepository = profileRepository;
        this.shopProductRepository = shopProductRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.productVariantRepository = productVariantRepository;
        this.variantAttributeTypeRepository = variantAttributeTypeRepository;
        this.variantAttributeValueRepository = variantAttributeValueRepository;
        this.shopProductImageRepository = shopProductImageRepository;
        this.productVariantImageRepository = productVariantImageRepository;
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
    @Transactional
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

        // Update attributeTypes if provided (replaces existing)
        if (request.getAttributeTypes() != null && !request.getAttributeTypes().isEmpty()) {
            sp.getAttributeTypes().clear();
            shopProductRepository.save(sp);
            Map<String, VariantAttributeType> typeMap = new HashMap<>();
            for (int i = 0; i < request.getAttributeTypes().size(); i++) {
                VariantAttributeType type = new VariantAttributeType();
                type.setAttributeName(request.getAttributeTypes().get(i));
                type.setDisplayOrder(i + 1);
                type.setShopProduct(sp);
                typeMap.put(type.getAttributeName(), variantAttributeTypeRepository.save(type));
            }
        }

        // Update variants if provided
        if (request.getVariants() != null) {
            // Collect IDs of variants sent in request (those with id = update, those without = add)
            Set<Long> incomingIds = request.getVariants().stream()
                .filter(vr -> vr.getId() != null)
                .map(VariantRequest::getId)
                .collect(Collectors.toSet());

            // Delete variants not in the incoming list
            List<ProductVariant> existing = productVariantRepository.findByParentShopProduct_Id(sp.getId());
            existing.stream()
                .filter(v -> !incomingIds.contains(v.getId()))
                .forEach(productVariantRepository::delete);

            // Rebuild typeMap from current saved attribute types
            Map<String, VariantAttributeType> typeMap = variantAttributeTypeRepository
                .findByShopProductIdOrderByDisplayOrder(sp.getId())
                .stream()
                .collect(Collectors.toMap(VariantAttributeType::getAttributeName, t -> t));

            for (VariantRequest vr : request.getVariants()) {
                ProductVariant variant;
                if (vr.getId() != null) {
                    variant = productVariantRepository.findById(vr.getId())
                        .orElseThrow(() -> new ApiException("Variant not found: " + vr.getId(), HttpStatus.NOT_FOUND));
                } else {
                    variant = new ProductVariant();
                    variant.setParentShopProduct(sp);
                    variant.setActive(true);
                }
                if (vr.getVariantName() != null) variant.setVariantName(vr.getVariantName());
                if (vr.getBrand() != null) variant.setBrand(vr.getBrand());
                if (vr.getShortDescription() != null) variant.setShortDescription(vr.getShortDescription());
                if (vr.getLongDescription() != null) variant.setLongDescription(vr.getLongDescription());
                if (vr.getMrp() != null) variant.setMrp(vr.getMrp());
                if (vr.getSellingPrice() != null) variant.setSellingPrice(vr.getSellingPrice());
                if (vr.getStockQuantity() != null) variant.setStockQuantity(vr.getStockQuantity());
                if (vr.getThresholdQuantity() != null) variant.setThresholdQuantity(vr.getThresholdQuantity());
                if (vr.getImageUrl() != null) variant.setImageUrl(vr.getImageUrl());
                if (vr.getExpiryDate() != null) variant.setExpiryDate(vr.getExpiryDate());
                if (vr.getGstSlab() != null) variant.setGstSlab(vr.getGstSlab());
                ProductVariant savedVariant = productVariantRepository.save(variant);
                if (savedVariant.getShopProductId() == null) {
                    savedVariant.setShopProductId(savedVariant.getId());
                    productVariantRepository.save(savedVariant);
                }

                if (vr.getAttributes() != null) {
                    List<VariantAttributeValue> existingValues = variantAttributeValueRepository.findByProductVariantId(savedVariant.getId());
                    for (Map.Entry<String, String> entry : vr.getAttributes().entrySet()) {
                        VariantAttributeType attrType = typeMap.get(entry.getKey());
                        if (attrType == null) continue;
                        VariantAttributeValue attrValue = existingValues.stream()
                            .filter(ev -> ev.getAttributeType().getId().equals(attrType.getId()))
                            .findFirst()
                            .orElse(new VariantAttributeValue());
                        attrValue.setAttributeType(attrType);
                        attrValue.setAttributeValue(entry.getValue());
                        attrValue.setProductVariant(savedVariant);
                        variantAttributeValueRepository.save(attrValue);
                    }
                }
            }
        }

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

        // Save attribute types (Size, Colour, etc.) — clear old ones first
        parent.getAttributeTypes().clear();
        shopProductRepository.save(parent);

        List<VariantAttributeType> savedTypes = new ArrayList<>();
        for (int i = 0; i < request.getAttributeTypes().size(); i++) {
            VariantAttributeType type = new VariantAttributeType();
            type.setAttributeName(request.getAttributeTypes().get(i));
            type.setDisplayOrder(i + 1);
            type.setShopProduct(parent);
            savedTypes.add(variantAttributeTypeRepository.save(type));
        }

        // Build a map of attributeName → VariantAttributeType for quick lookup
        Map<String, VariantAttributeType> typeMap = savedTypes.stream()
            .collect(Collectors.toMap(VariantAttributeType::getAttributeName, t -> t));

        List<ProductVariant> saved = new ArrayList<>();
        for (VariantRequest vr : request.getVariants()) {
            ProductVariant variant = new ProductVariant();
            variant.setParentShopProduct(parent);
            variant.setVariantName(vr.getVariantName());
            variant.setBrand(vr.getBrand());
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
            ProductVariant savedVariant = productVariantRepository.save(variant);

            // Save attribute values (e.g. Size=8, Colour=Red)
            if (vr.getAttributes() != null) {
                for (Map.Entry<String, String> entry : vr.getAttributes().entrySet()) {
                    VariantAttributeType attrType = typeMap.get(entry.getKey());
                    if (attrType == null) continue;
                    VariantAttributeValue attrValue = new VariantAttributeValue();
                    attrValue.setAttributeType(attrType);
                    attrValue.setAttributeValue(entry.getValue());
                    attrValue.setProductVariant(savedVariant);
                    variantAttributeValueRepository.save(attrValue);
                }
            }
            saved.add(savedVariant);
        }

        // Auto-set shopProductId = variant's generated id
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
    @Transactional
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

        if (request.isHasVariants() && request.getAttributeTypes() != null && !request.getAttributeTypes().isEmpty()) {
            Map<String, VariantAttributeType> typeMap = new HashMap<>();
            for (int i = 0; i < request.getAttributeTypes().size(); i++) {
                VariantAttributeType type = new VariantAttributeType();
                type.setAttributeName(request.getAttributeTypes().get(i));
                type.setDisplayOrder(i + 1);
                type.setShopProduct(saved);
                typeMap.put(type.getAttributeName(), variantAttributeTypeRepository.save(type));
            }

            if (request.getVariants() != null) {
                for (VariantRequest vr : request.getVariants()) {
                    ProductVariant variant = new ProductVariant();
                    variant.setParentShopProduct(saved);
                    variant.setVariantName(vr.getVariantName());
                    variant.setBrand(vr.getBrand());
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
                    ProductVariant savedVariant = productVariantRepository.save(variant);
                    savedVariant.setShopProductId(savedVariant.getId());
                    productVariantRepository.save(savedVariant);

                    if (vr.getAttributes() != null) {
                        for (Map.Entry<String, String> entry : vr.getAttributes().entrySet()) {
                            VariantAttributeType attrType = typeMap.get(entry.getKey());
                            if (attrType == null) continue;
                            VariantAttributeValue attrValue = new VariantAttributeValue();
                            attrValue.setAttributeType(attrType);
                            attrValue.setAttributeValue(entry.getValue());
                            attrValue.setProductVariant(savedVariant);
                            variantAttributeValueRepository.save(attrValue);
                        }
                    }
                }
            }
        }

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
        r.setImageUrls(shopProductImageRepository
                .findByShopProductIdOrderByDisplayOrderAsc(sp.getId())
                .stream().map(ShopProductImage::getImageUrl).toList());
        r.setShortDescription(sp.getShortDescription());
        r.setExpiryDate(sp.getExpiryDate());
        r.setHasVariants(sp.isHasVariants());
        r.setActive(sp.isActive());
        r.setUnlisted(sp.getMasterProduct() == null);
        if (sp.isHasVariants()) {
            // Set attribute type names (e.g. ["Size", "Colour"])
            List<String> attrTypeNames = variantAttributeTypeRepository
                .findByShopProductIdOrderByDisplayOrder(sp.getId())
                .stream()
                .map(VariantAttributeType::getAttributeName)
                .collect(Collectors.toList());
            r.setAttributeTypes(attrTypeNames);

            List<VariantResponse> variants = productVariantRepository.findByParentShopProduct_Id(sp.getId())
                .stream().map(v -> {
                    VariantResponse vr = new VariantResponse();
                    vr.setId(v.getId());
                    vr.setVariantName(v.getVariantName());
                    vr.setBrand(v.getBrand());
                    vr.setMrp(v.getMrp());
                    vr.setSellingPrice(v.getSellingPrice());
                    vr.setStockQuantity(v.getStockQuantity());
                    vr.setThresholdQuantity(v.getThresholdQuantity());
                    vr.setImageUrl(v.getImageUrl());
                    vr.setImageUrls(productVariantImageRepository
                            .findByVariantIdOrderByDisplayOrderAsc(v.getId())
                            .stream().map(ProductVariantImage::getImageUrl).toList());
                    vr.setExpiryDate(v.getExpiryDate());
                    vr.setActive(v.isActive());
                    // Build attributes map e.g. { "Size": "8", "Colour": "Red" }
                    Map<String, String> attrs = variantAttributeValueRepository
                        .findByProductVariantId(v.getId())
                        .stream()
                        .collect(Collectors.toMap(
                            val -> val.getAttributeType().getAttributeName(),
                            VariantAttributeValue::getAttributeValue
                        ));
                    vr.setAttributes(attrs);
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

        if (!variant.getParentShopProduct().getShop().getId().equals(shop.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
        if (request.getBrand() != null) variant.setBrand(request.getBrand());
        if (request.getShortDescription() != null) variant.setShortDescription(request.getShortDescription());
        if (request.getLongDescription() != null) variant.setLongDescription(request.getLongDescription());
        if (request.getMrp() != null) variant.setMrp(request.getMrp());
        if (request.getSellingPrice() != null) variant.setSellingPrice(request.getSellingPrice());
        if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
        if (request.getThresholdQuantity() != null) variant.setThresholdQuantity(request.getThresholdQuantity());
        if (request.getImageUrl() != null) variant.setImageUrl(request.getImageUrl());
        if (request.getExpiryDate() != null) variant.setExpiryDate(request.getExpiryDate());
        if (request.getGstSlab() != null) variant.setGstSlab(request.getGstSlab());

        // Update attribute values if provided
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            List<VariantAttributeValue> existingValues = variantAttributeValueRepository.findByProductVariantId(variantId);
            for (VariantAttributeValue existing : existingValues) {
                String attrName = existing.getAttributeType().getAttributeName();
                if (request.getAttributes().containsKey(attrName)) {
                    existing.setAttributeValue(request.getAttributes().get(attrName));
                    variantAttributeValueRepository.save(existing);
                }
            }
        }

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

