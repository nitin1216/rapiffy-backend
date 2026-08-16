package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CustomerCatalogResponse;
import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.CustomerVariantResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;

import java.util.List;
import java.util.Map;

public interface CustomerShopService {

    List<NearbyShopResponse> getNearbyShops(double lat, double lng);

    List<CustomerCatalogResponse> getShopCatalog(Long shopId);

    List<CustomerCatalogResponse> getAggregatedCatalog(double lat, double lng);

    List<CustomerCatalogResponse> getCatalogByCategory(double lat, double lng, Long categoryId);

    CustomerProductResponse getProductById(Long shopProductId);

    List<CustomerVariantResponse> getVariantsByParentShopProductId(Long shopProductId);

    Map<String, List<String>> getProductAttributes(Long shopProductId);

    List<CustomerVariantResponse> filterVariants(Long shopProductId, Map<String, String> selectedAttributes);
}
