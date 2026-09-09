package com.example.rapiffy.controller;

import com.example.rapiffy.dto.admin.returns.AdminReviewReturnRequest;
import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Returns", description = "APIs for admin to review and action customer return requests")
@RequestMapping("v1/admin/returns")
public interface AdminReturnController {

    @Operation(
        summary = "Get all return requests for my shop",
        description = "Returns all return requests for orders belonging to the logged-in admin's shop, newest first."
    )
    @GetMapping
    ResponseEntity<List<ReturnRequestResponse>> getReturns();

    @Operation(
        summary = "Approve or reject a return request",
        description = "approved=true → refund credited to customer wallet instantly. approved=false → adminNote is required."
    )
    @PutMapping("/{returnRequestId}/review")
    ResponseEntity<ReturnRequestResponse> reviewReturn(
            @PathVariable Long returnRequestId,
            @Valid @RequestBody AdminReviewReturnRequest request
    );
}
