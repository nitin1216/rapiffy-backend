package com.example.rapiffy.controller;

import com.example.rapiffy.dto.admin.cancellation.AdminReviewCancellationRequest;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Cancellations", description = "APIs for admin to review and action customer cancellation requests")
@RequestMapping("v1/admin/cancellations")
public interface AdminCancellationController {

    @Operation(
        summary = "Get all cancellation requests for my shop",
        description = "Returns all cancellation requests for orders belonging to the logged-in admin's shop, newest first."
    )
    @GetMapping
    ResponseEntity<List<CancellationRequestResponse>> getCancellations();

    @Operation(
        summary = "Approve or reject a cancellation request",
        description = "approved=true → items are cancelled and refund is processed per payment method. approved=false → adminNote is required."
    )
    @PutMapping("/{cancellationRequestId}/review")
    ResponseEntity<CancellationRequestResponse> reviewCancellation(
            @PathVariable Long cancellationRequestId,
            @Valid @RequestBody AdminReviewCancellationRequest request
    );
}
