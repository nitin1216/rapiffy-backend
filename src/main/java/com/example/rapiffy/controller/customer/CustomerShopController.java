package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.CustomerCatalogResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;
import com.example.rapiffy.services.customer.CustomerShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer - Shops & Products", description = "Public APIs for browsing nearby shops and products. No login required.")
@RestController
@RequestMapping("/v1/customer")
@RequiredArgsConstructor
public class CustomerShopController {

    private final CustomerShopService customerShopService;

    @Operation(
        summary = "Get nearby shops",
        description = "Returns all shops within their serving range from the customer's location. Sorted by nearest first."
    )
    @GetMapping("/shops")
    public ResponseEntity<List<NearbyShopResponse>> getNearbyShops(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(customerShopService.getNearbyShops(lat, lng));
    }

    @Operation(
        summary = "Get catalog of a specific shop",
        description = "Returns active products as a tree: Category → SubCategory → Products."
    )
    @GetMapping("/shops/{shopId}/catalog")
    public ResponseEntity<List<CustomerCatalogResponse>> getShopCatalog(@PathVariable Long shopId) {
        return ResponseEntity.ok(customerShopService.getShopCatalog(shopId));
    }

    @Operation(
        summary = "Get aggregated catalog from all nearby shops",
        description = "Returns active products from ALL nearby shops as a tree: Category → SubCategory → Products."
    )
    @GetMapping("/catalog")
    public ResponseEntity<List<CustomerCatalogResponse>> getAggregatedCatalog(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(customerShopService.getAggregatedCatalog(lat, lng));
    }
}
