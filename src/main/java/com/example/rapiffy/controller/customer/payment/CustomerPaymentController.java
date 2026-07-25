package com.example.rapiffy.controller.customer.payment;

import com.example.rapiffy.dto.customer.payment.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer - Payment", description = "Payment, cancellation & refund APIs. Login required.")
@RestController
@RequestMapping("/v1/customer/payment")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final CustomerPaymentService paymentService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Initiate payment",
        description = "Creates a Razorpay order for the given parent order. "
            + "Returns razorpayOrderId, amount, and key — frontend uses these to open Razorpay checkout."
    )
    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiatePayment(
            @Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(getCurrentUserId(), request));
    }

    @Operation(
        summary = "Verify payment",
        description = "Called by frontend after customer completes payment. "
            + "Verifies Razorpay signature, marks payment as PAID, updates order status, "
            + "and creates transfers to split money to each shop."
    )
    @PostMapping("/verify")
    public ResponseEntity<PaymentStatusResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(getCurrentUserId(), request));
    }

    @Operation(
        summary = "Get payment status",
        description = "Returns current payment status for a parent order. "
            + "Shows whether payment is pending, paid, partially refunded, or fully refunded."
    )
    @GetMapping("/{parentOrderId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable Long parentOrderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(getCurrentUserId(), parentOrderId));
    }

    @Operation(
        summary = "Cancel a sub-order",
        description = "Cancels a specific shop's sub-order and triggers a refund for that amount. "
            + "Only allowed if the sub-order is still in PENDING status (admin hasn't confirmed yet). "
            + "Refund is processed back to the original payment method."
    )
    @PostMapping("/cancel/{subOrderId}")
    public ResponseEntity<PaymentStatusResponse> cancelSubOrder(
            @PathVariable Long subOrderId,
            @Valid @RequestBody CancelSubOrderRequest request) {
        return ResponseEntity.ok(paymentService.cancelSubOrder(getCurrentUserId(), subOrderId, request));
    }

    @Operation(
        summary = "Get refund history",
        description = "Returns all refunds for a parent order. "
            + "Shows which sub-orders were cancelled, refund amounts, and their current status "
            + "(initiated, processing, completed)."
    )
    @GetMapping("/refunds/{parentOrderId}")
    public ResponseEntity<RefundHistoryResponse> getRefundHistory(
            @PathVariable Long parentOrderId) {
        return ResponseEntity.ok(paymentService.getRefundHistory(getCurrentUserId(), parentOrderId));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
