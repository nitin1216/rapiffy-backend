package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.CustomerCatalogResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;

import java.util.List;

public interface CustomerShopService {

    List<NearbyShopResponse> getNearbyShops(double lat, double lng);

    // Tree: Category → SubCategory → Products for a specific shop
    List<CustomerCatalogResponse> getShopCatalog(Long shopId);

    // Tree: Category → SubCategory → Products aggregated from all nearby shops
    List<CustomerCatalogResponse> getAggregatedCatalog(double lat, double lng);
}
