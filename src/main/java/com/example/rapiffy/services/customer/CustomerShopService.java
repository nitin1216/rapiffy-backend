package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;

import java.util.List;

public interface CustomerShopService {

    List<NearbyShopResponse> getNearbyShops(double lat, double lng);

    List<CustomerProductResponse> getShopProducts(Long shopId, Long categoryId);

    List<CustomerProductResponse> getAggregatedProducts(double lat, double lng, Long categoryId);
}
