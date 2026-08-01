package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.ShopProduct;
import com.example.rapiffy.model.SubCategory;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.ShopProductRepository;
import com.example.rapiffy.services.customer.CustomerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerShopServiceImpl implements CustomerShopService {

    private final ProfileRepository profileRepository;
    private final ShopProductRepository shopProductRepository;

    // ── NEARBY SHOPS ─────────────────────────────────────────────────────────

    @Override
    public List<NearbyShopResponse> getNearbyShops(double customerLat, double customerLng) {
        return profileRepository.findAllByUserRole(Roles.ADMIN)
                .stream()
                .filter(shop -> hasValidLocation(shop) && hasServingRange(shop))
                .map(shop -> toNearbyShopResponse(shop, customerLat, customerLng))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(NearbyShopResponse::getDistanceInKm))
                .toList();
    }

    // ── SHOP CATALOG (tree) ───────────────────────────────────────────────────

    @Override
    public List<CustomerCatalogResponse> getShopCatalog(Long shopId) {
        List<ShopProduct> products = shopProductRepository.findByShopIdAndIsActive(shopId, true);
        return buildCatalogTree(products, null, null);
    }

    // ── AGGREGATED CATALOG (tree from all nearby shops) ───────────────────────

    @Override
    public List<CustomerCatalogResponse> getAggregatedCatalog(double lat, double lng) {
        List<ShopProduct> allProducts = profileRepository.findAllByUserRole(Roles.ADMIN)
                .stream()
                .filter(shop -> hasValidLocation(shop) && hasServingRange(shop))
                .filter(shop -> {
                    double shopLat = Double.parseDouble(shop.getAddress().getLatitude());
                    double shopLng = Double.parseDouble(shop.getAddress().getLongitude());
                    return haversine(lat, lng, shopLat, shopLng) <= shop.getServingRangeInKm();
                })
                .flatMap(shop -> {
                    double shopLat = Double.parseDouble(shop.getAddress().getLatitude());
                    double shopLng = Double.parseDouble(shop.getAddress().getLongitude());
                    double distance = Math.round(haversine(lat, lng, shopLat, shopLng) * 100.0) / 100.0;
                    return shopProductRepository.findByShopIdAndIsActive(shop.getId(), true)
                            .stream()
                            .peek(p -> p.getShop()); // ensure shop is loaded
                })
                .collect(Collectors.toList());

        return buildCatalogTree(allProducts, lat, lng);
    }

    // ── TREE BUILDER ──────────────────────────────────────────────────────────

    /**
     * Builds Category → SubCategory → Products tree from a flat list of ShopProducts.
     * lat/lng are optional — passed through to product response for distance display.
     */
    private List<CustomerCatalogResponse> buildCatalogTree(List<ShopProduct> products, Double lat, Double lng) {
        // Group by categoryName → subCategoryName → products
        Map<String, Map<String, List<ShopProduct>>> tree = new LinkedHashMap<>();

        for (ShopProduct sp : products) {
            SubCategory sc = sp.getSubCategory();
            if (sc == null) continue;
            String catName = sc.getCategory().getCategoryName();
            String subCatName = sc.getName();
            tree.computeIfAbsent(catName, k -> new LinkedHashMap<>())
                .computeIfAbsent(subCatName, k -> new ArrayList<>())
                .add(sp);
        }

        return tree.entrySet().stream().map(catEntry -> {
            ShopProduct first = catEntry.getValue().values().iterator().next().get(0);

            CustomerCatalogResponse cat = new CustomerCatalogResponse();
            cat.setCategoryId(first.getSubCategory().getCategory().getId());
            cat.setCategoryName(first.getSubCategory().getCategory().getCategoryName());

            List<CustomerCatalogResponse.SubCategoryResponse> subList = catEntry.getValue().entrySet().stream()
                .map(subEntry -> {
                    ShopProduct subFirst = subEntry.getValue().get(0);
                    CustomerCatalogResponse.SubCategoryResponse sub = new CustomerCatalogResponse.SubCategoryResponse();
                    sub.setSubCategoryId(subFirst.getSubCategory().getId());
                    sub.setSubCategoryName(subFirst.getSubCategory().getName());
                    sub.setImageUrl(subFirst.getSubCategory().getImageUrl());
                    sub.setProducts(subEntry.getValue().stream()
                        .map(p -> toProductResponse(p, lat, lng))
                        .collect(Collectors.toList()));
                    return sub;
                }).collect(Collectors.toList());

            cat.setSubCategories(subList);
            return cat;
        }).collect(Collectors.toList());
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private NearbyShopResponse toNearbyShopResponse(Profile shop, double customerLat, double customerLng) {
        double shopLat = Double.parseDouble(shop.getAddress().getLatitude());
        double shopLng = Double.parseDouble(shop.getAddress().getLongitude());
        double distance = haversine(customerLat, customerLng, shopLat, shopLng);

        if (distance > shop.getServingRangeInKm()) return null;

        List<String> categories = shop.getShopCategories().stream()
                .map(c -> c.getCategoryName()).distinct().toList();

        return NearbyShopResponse.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .city(shop.getAddress().getCity())
                .addressLine1(shop.getAddress().getAddressLine1())
                .distanceInKm(Math.round(distance * 100.0) / 100.0)
                .servingRangeInKm(shop.getServingRangeInKm())
                .categories(categories)
                .build();
    }

    private CustomerProductResponse toProductResponse(ShopProduct p, Double lat, Double lng) {
        Double distance = null;
        if (lat != null && lng != null && p.getShop().getAddress() != null) {
            try {
                double shopLat = Double.parseDouble(p.getShop().getAddress().getLatitude());
                double shopLng = Double.parseDouble(p.getShop().getAddress().getLongitude());
                distance = Math.round(haversine(lat, lng, shopLat, shopLng) * 100.0) / 100.0;
            } catch (NumberFormatException ignored) {}
        }

        List<CustomerVariantResponse> variants = p.isHasVariants()
                ? p.getVariants().stream()
                    .filter(v -> v.isActive())
                    .map(v -> CustomerVariantResponse.builder()
                            .variantId(v.getId())
                            .variantName(v.getVariantName())
                            .brand(v.getBrand())
                            .unit(v.getUnit())
                            .unitValue(v.getUnitValue())
                            .mrp(v.getMrp())
                            .sellingPrice(v.getSellingPrice())
                            .stockQuantity(v.getStockQuantity())
                            .imageUrl(v.getImageUrl())
                            .build())
                    .toList()
                : null;

        return CustomerProductResponse.builder()
                .shopProductId(p.getId())
                .productName(p.getProductName())
                .brand(p.getBrand())
                .unit(p.getUnit())
                .unitValue(p.getUnitValue())
                .mrp(p.getMrp())
                .sellingPrice(p.getSellingPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .shortDescription(p.getShortDescription())
                .subCategoryId(p.getSubCategory() != null ? p.getSubCategory().getId() : null)
                .subCategoryName(p.getSubCategory() != null ? p.getSubCategory().getName() : null)
                .categoryId(p.getSubCategory() != null ? p.getSubCategory().getCategory().getId() : null)
                .categoryName(p.getSubCategory() != null ? p.getSubCategory().getCategory().getCategoryName() : null)
                .hasVariants(p.isHasVariants())
                .variants(variants)
                .shopId(p.getShop().getId())
                .shopName(p.getShop().getShopName())
                .distanceInKm(distance)
                .build();
    }

    // ── UTILS ─────────────────────────────────────────────────────────────────

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean hasValidLocation(Profile shop) {
        if (shop.getAddress() == null) return false;
        String lat = shop.getAddress().getLatitude();
        String lng = shop.getAddress().getLongitude();
        if (lat == null || lat.isBlank() || lng == null || lng.isBlank()) return false;
        try {
            Double.parseDouble(lat);
            Double.parseDouble(lng);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean hasServingRange(Profile shop) {
        return shop.getServingRangeInKm() != null && shop.getServingRangeInKm() > 0;
    }
}
