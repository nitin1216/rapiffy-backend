package com.example.rapiffy.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic response for catalog operations (activate, update, deactivate, add).
 */
@Data
@AllArgsConstructor
public class CatalogActionResponse {

    private Long shopProductId;
    private String message;
}
