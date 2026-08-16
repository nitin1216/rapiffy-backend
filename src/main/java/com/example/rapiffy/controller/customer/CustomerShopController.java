package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.CustomerCatalogResponse;
import com.example.rapiffy.dto.customer.CustomerProductResponse;
import com.example.rapiffy.dto.customer.CustomerVariantResponse;
import com.example.rapiffy.dto.customer.NearbyShopResponse;
import com.example.rapiffy.dto.customer.VariantFilterRequest;
import com.example.rapiffy.model.Category;
import com.example.rapiffy.repos.CategoryRepository;
import com.example.rapiffy.services.customer.CustomerShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Customer - Shops & Products", description = "Public APIs for browsing nearby shops and products. No login required.")
@RestController
@RequestMapping("/v1/customer")
@RequiredArgsConstructor
public class CustomerShopController {

    private final CustomerShopService customerShopService;
    private final CategoryRepository categoryRepository;

    @Operation(summary = "Get all active categories",
        description = "Returns all active categories to display as tabs (All, Grocery, Medical, etc.)")
    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getCategories() {
        return ResponseEntity.ok(categoryRepository.findAll().stream()
            .filter(Category::isActive)
            .toList());
    }

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

    @Operation(
        summary = "Get nearby products filtered by category",
        description = "Returns products from nearby shops for a specific category. Use for All=no categoryId, Grocery/Medical/etc=pass categoryId."
    )
    @GetMapping("/catalog/category/{categoryId}")
    public ResponseEntity<List<CustomerCatalogResponse>> getCatalogByCategory(
            @RequestParam double lat,
            @RequestParam double lng,
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(customerShopService.getCatalogByCategory(lat, lng, categoryId));
    }

    @Operation(
        summary = "Get product by ID",
        description = "Returns full product details for a given shop product ID."
    )
    @GetMapping("/products/{shopProductId}")
    public ResponseEntity<CustomerProductResponse> getProductById(@PathVariable Long shopProductId) {
        return ResponseEntity.ok(customerShopService.getProductById(shopProductId));
    }

    @Operation(
        summary = "Get all variants of a product",
        description = "Returns all active variants for a given shopProductId. Each variant has its own shopProductId to use for cart/order."
    )
    @GetMapping("/products/{shopProductId}/variants")
    public ResponseEntity<List<CustomerVariantResponse>> getVariantsByProduct(@PathVariable Long shopProductId) {
        return ResponseEntity.ok(customerShopService.getVariantsByParentShopProductId(shopProductId));
    }

    @Operation(
        summary = "Get all available attribute values for a product",
        description = "Returns all distinct values per attribute (e.g. Size: [8,9,10], Colour: [Red,Black]). Use on initial page load."
    )
    @GetMapping("/products/{shopProductId}/attributes")
    public ResponseEntity<Map<String, List<String>>> getProductAttributes(@PathVariable Long shopProductId) {
        return ResponseEntity.ok(customerShopService.getProductAttributes(shopProductId));
    }

    @Operation(
        summary = "Filter variants by selected attributes",
        description = "Pass selected attributes (e.g. {Size:8}) to get matching variants. Returns exact variant when all attributes are selected."
    )
    @PostMapping("/products/{shopProductId}/variants/filter")
    public ResponseEntity<List<CustomerVariantResponse>> filterVariants(
            @PathVariable Long shopProductId,
            @RequestBody VariantFilterRequest request) {
        return ResponseEntity.ok(customerShopService.filterVariants(shopProductId, request.getAttributes()));
    }
}
