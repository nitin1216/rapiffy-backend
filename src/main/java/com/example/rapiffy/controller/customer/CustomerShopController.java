package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.CustomerProductResponse;
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
        description = "Returns all shops within their serving range from the customer's location. "
            + "Uses Haversine formula to calculate distance. Sorted by nearest first."
    )
    @GetMapping("/shops")
    public ResponseEntity<List<NearbyShopResponse>> getNearbyShops(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(customerShopService.getNearbyShops(lat, lng));
    }

    @Operation(
        summary = "Get products of a specific shop",
        description = "Returns all active products for a given shop. "
            + "Optionally filter by categoryId. Only active products are returned."
    )
    @GetMapping("/shops/{shopId}/products")
    public ResponseEntity<List<CustomerProductResponse>> getShopProducts(
            @PathVariable Long shopId,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(customerShopService.getShopProducts(shopId, categoryId));
    }

    @Operation(
        summary = "Get aggregated products from all nearby shops",
        description = "Returns active products from ALL shops within the customer's location range. "
            + "Each product includes shopId and shopName so the customer knows which shop it's from. "
            + "Optionally filter by categoryId."
    )
    @GetMapping("/products")
    public ResponseEntity<List<CustomerProductResponse>> getAggregatedProducts(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(customerShopService.getAggregatedProducts(lat, lng, categoryId));
    }
}
