package com.example.rapiffy.impl.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.CustomerVariantResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.ShopProduct;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.ShopProductRepository;
import com.example.rapiffy.services.customer.CustomerShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerShopServiceImpl implements CustomerShopService {

    private final ProfileRepository profileRepository;
    private final ShopProductRepository shopProductRepository;

    @Override
    public List<NearbyShopResponse> getNearbyShops(double customerLat, double customerLng) {
        return profileRepository.findAllByUserRole(Roles.ADMIN)
                .stream()
                .filter(shop -> hasValidLocation(shop) && hasServingRange(shop))
                .map(shop -> toResponse(shop, customerLat, customerLng))
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(a.getDistanceInKm(), b.getDistanceInKm()))
                .toList();
    }

    private NearbyShopResponse toResponse(Profile shop, double customerLat, double customerLng) {
        double shopLat = Double.parseDouble(shop.getAddress().getLatitude());
        double shopLng = Double.parseDouble(shop.getAddress().getLongitude());
        double distance = haversine(customerLat, customerLng, shopLat, shopLng);

        if (distance > shop.getServingRangeInKm()) return null;

        List<String> categories = shop.getShopCategories()
                .stream()
                .map(c -> c.getCategoryName())
                .distinct()
                .toList();

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

    // Returns distance in kilometers between two lat/lng points
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

    @Override
    public List<CustomerProductResponse> getShopProducts(Long shopId, Long categoryId) {
        List<ShopProduct> products = (categoryId != null)
                ? shopProductRepository.findByShopIdAndCategoryIdAndIsActive(shopId, categoryId, true)
                : shopProductRepository.findByShopIdAndIsActive(shopId, true);

        return products.stream().map(p -> toProductResponse(p, null, null)).toList();
    }

    @Override
    public List<CustomerProductResponse> getAggregatedProducts(double lat, double lng, Long categoryId) {
        return profileRepository.findAllByUserRole(Roles.ADMIN)
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

                    List<ShopProduct> products = (categoryId != null)
                            ? shopProductRepository.findByShopIdAndCategoryIdAndIsActive(shop.getId(), categoryId, true)
                            : shopProductRepository.findByShopIdAndIsActive(shop.getId(), true);

                    return products.stream().map(p -> toProductResponse(p, shop, distance));
                })
                .toList();
    }

    private CustomerProductResponse toProductResponse(ShopProduct p, Profile shop, Double distance) {
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
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .hasVariants(p.isHasVariants())
                .variants(variants)
                .shopId(shop != null ? shop.getId() : p.getShop().getId())
                .shopName(shop != null ? shop.getShopName() : p.getShop().getShopName())
                .distanceInKm(distance)
                .build();
    }
}
