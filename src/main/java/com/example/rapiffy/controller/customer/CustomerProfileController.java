package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer - Profile", description = "Customer profile and address management. Login required.")
@RestController
@RequestMapping("/v1/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService profileService;
    private final UserRepository userRepository;

    @Operation(summary = "Get my profile", description = "Returns logged-in customer's profile details.")
    @GetMapping
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(getCurrentUserId()));
    }

    @Operation(summary = "Update my profile", description = "Update name or email. Phone number cannot be changed.")
    @PutMapping
    public ResponseEntity<CustomerProfileResponse> updateProfile(@RequestBody UpdateCustomerProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(getCurrentUserId(), request));
    }

    @Operation(summary = "Get all saved addresses", description = "Returns all saved addresses. Default address is marked with isDefault = true.")
    @GetMapping("/addresses")
    public ResponseEntity<List<CustomerAddressResponse>> getAddresses() {
        return ResponseEntity.ok(profileService.getAddresses(getCurrentUserId()));
    }

    @Operation(
        summary = "Add new address",
        description = "Adds a new address. If it's the first address or isDefault = true, it becomes the default. "
            + "Only one address can be default at a time."
    )
    @PostMapping("/addresses")
    public ResponseEntity<CustomerAddressResponse> addAddress(@Valid @RequestBody SaveAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(profileService.addAddress(getCurrentUserId(), request));
    }

    @Operation(summary = "Update an address", description = "Updates an existing address. Set isDefault = true to make it the default.")
    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<CustomerAddressResponse> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody SaveAddressRequest request) {
        return ResponseEntity.ok(profileService.updateAddress(getCurrentUserId(), addressId, request));
    }

    @Operation(
        summary = "Delete an address",
        description = "Deletes an address. If the deleted address was default, the next available address becomes default automatically."
    )
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        profileService.deleteAddress(getCurrentUserId(), addressId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Set default address",
        description = "Sets a specific address as default. All other addresses are automatically set to non-default. "
            + "Returns updated list of all addresses."
    )
    @PutMapping("/addresses/{addressId}/default")
    public ResponseEntity<List<CustomerAddressResponse>> setDefaultAddress(@PathVariable Long addressId) {
        return ResponseEntity.ok(profileService.setDefaultAddress(getCurrentUserId(), addressId));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
