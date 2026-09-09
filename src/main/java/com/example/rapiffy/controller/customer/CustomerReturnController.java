package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;
import com.example.rapiffy.dto.customer.returns.SubmitReturnRequest;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.ReturnImage;
import com.example.rapiffy.model.ReturnRequest;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.ReturnImageRepository;
import com.example.rapiffy.repos.ReturnRequestRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerReturnService;
import com.example.rapiffy.sftp.SftpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Customer - Returns", description = "APIs for raising return requests on delivered orders")
@RestController
@RequestMapping("/v1/customer/returns")
@RequiredArgsConstructor
public class CustomerReturnController {

    private final CustomerReturnService returnService;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnImageRepository returnImageRepository;
    private final SftpService sftpService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Submit a return request",
        description = "Customer selects items from a delivered sub-order, picks a reason (MISSING/EXPIRED/OVERSIZED/DAMAGED), and submits. Images must be uploaded separately via the upload endpoint."
    )
    @PostMapping("/orders/{subOrderId}")
    public ResponseEntity<ReturnRequestResponse> submitReturn(
            @PathVariable Long subOrderId,
            @Valid @RequestBody SubmitReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnService.submitReturn(getCurrentUserId(), subOrderId, request));
    }

    @Operation(
        summary = "Upload proof images for a return request",
        description = "Upload 1 to 5 images as proof. Must be called after submitReturn. Images stored on SFTP."
    )
    @PostMapping(value = "/{returnRequestId}/images", consumes = "multipart/form-data")
    public ResponseEntity<List<String>> uploadReturnImages(
            @PathVariable Long returnRequestId,
            @RequestParam("files") List<MultipartFile> files) {

        Long userId = getCurrentUserId();

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ApiException("Return request not found", HttpStatus.NOT_FOUND));

        if (!returnRequest.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (files.size() > 5)
            throw new ApiException("Maximum 5 images allowed per return request", HttpStatus.BAD_REQUEST);

        int nextOrder = returnImageRepository.findByReturnRequestIdOrderByDisplayOrderAsc(returnRequestId).size();

        for (MultipartFile file : files) {
            String imageUrl = sftpService.uploadImage(file, "returns/" + returnRequestId);
            ReturnImage image = new ReturnImage();
            image.setReturnRequest(returnRequest);
            image.setImageUrl(imageUrl);
            image.setDisplayOrder(nextOrder++);
            returnImageRepository.save(image);
        }

        List<String> imageUrls = returnImageRepository
                .findByReturnRequestIdOrderByDisplayOrderAsc(returnRequestId)
                .stream().map(ReturnImage::getImageUrl).toList();

        return ResponseEntity.ok(imageUrls);
    }

    @Operation(summary = "Get my return requests", description = "Returns all return requests raised by the logged-in customer, newest first.")
    @GetMapping
    public ResponseEntity<List<ReturnRequestResponse>> getMyReturns() {
        return ResponseEntity.ok(returnService.getMyReturns(getCurrentUserId()));
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
