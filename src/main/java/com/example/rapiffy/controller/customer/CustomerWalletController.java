package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.wallet.WalletResponse;
import com.example.rapiffy.dto.customer.wallet.WalletTopupRequest;
import com.example.rapiffy.dto.customer.wallet.WalletTopupResponse;
import com.example.rapiffy.dto.customer.wallet.WalletTopupVerifyRequest;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Customer - Wallet", description = "APIs for wallet balance, top-up and transaction history")
@RestController
@RequestMapping("/v1/customer/wallet")
@RequiredArgsConstructor
public class CustomerWalletController {

    private final CustomerReturnService returnService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Get wallet balance and transaction history",
        description = "Returns current wallet balance and full transaction history (top-ups, refunds, deductions)."
    )
    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return ResponseEntity.ok(returnService.getWallet(getCurrentUserId()));
    }

    @Operation(
        summary = "Initiate wallet top-up",
        description = "Creates a Razorpay order for the given amount. Returns razorpayOrderId, amount, and key — frontend uses these to open Razorpay checkout."
    )
    @PostMapping("/topup/initiate")
    public ResponseEntity<WalletTopupResponse> initiateWalletTopup(
            @Valid @RequestBody WalletTopupRequest request) {
        return ResponseEntity.ok(returnService.initiateWalletTopup(getCurrentUserId(), request));
    }

    @Operation(
        summary = "Verify wallet top-up",
        description = "Called after customer completes payment on Razorpay. Verifies signature and credits the amount to the customer's in-app wallet."
    )
    @PostMapping("/topup/verify")
    public ResponseEntity<WalletResponse> verifyWalletTopup(
            @Valid @RequestBody WalletTopupVerifyRequest request) {
        return ResponseEntity.ok(returnService.verifyWalletTopup(getCurrentUserId(), request));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
