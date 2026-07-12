package com.example.rapiffy.dto.superadmin;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO after CSV import — shows how many products were imported/skipped.
 */
@Data
@AllArgsConstructor
public class CsvImportResponse {

    private int totalRows;
    private int imported;
    private int skipped;  // duplicates or invalid rows
    private String message;
}
