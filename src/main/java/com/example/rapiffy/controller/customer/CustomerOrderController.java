package com.example.rapiffy.controller.customer;

import com.example.rapiffy.dto.customer.CustomerInvoiceResponse;
import com.example.rapiffy.dto.customer.CustomerOrderSummaryResponse;
import com.example.rapiffy.dto.customer.ParentOrderResponse;
import com.example.rapiffy.dto.customer.PlaceOrderRequest;
import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.impl.customer.CustomerInvoicePdfService;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Customer - Orders", description = "APIs for placing and tracking orders. Login required.")
@RestController
@RequestMapping("/v1/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final UserRepository userRepository;
    private final CustomerInvoicePdfService customerInvoicePdfService;

    @Operation(
        summary = "Place a new order",
        description = "Customer sends full cart items from one or multiple shops. "
            + "Backend automatically groups items by shopId and creates one sub-order per shop. "
            + "Single delivery address shared across all sub-orders. Returns full parent order with all sub-orders."
    )
    @PostMapping("/place")
    public ResponseEntity<ParentOrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerOrderService.placeOrder(getCurrentUserId(), request));
    }

    @Operation(
        summary = "Get my orders",
        description = "Returns all parent orders placed by the logged-in customer, sorted by latest first. "
            + "Each entry shows combined shop names and total amount."
    )
    @GetMapping
    public ResponseEntity<List<CustomerOrderSummaryResponse>> getMyOrders() {
        return ResponseEntity.ok(customerOrderService.getMyOrders(getCurrentUserId()));
    }

    @Operation(
        summary = "Get order detail",
        description = "Returns full parent order with all sub-orders and their items. "
            + "Each sub-order shows its own shop, status, and items. Customer can only access their own orders."
    )
    @GetMapping("/{parentOrderId}")
    public ResponseEntity<ParentOrderResponse> getOrderDetail(@PathVariable Long parentOrderId) {
        return ResponseEntity.ok(customerOrderService.getOrderDetail(getCurrentUserId(), parentOrderId));
    }

    @Operation(
        summary = "Get sub-order invoice as JSON",
        description = "Returns invoice for a single shop's sub-order. Available once the shop confirms the order."
    )
    @GetMapping("/{parentOrderId}/suborders/{subOrderId}/invoice")
    public ResponseEntity<CustomerInvoiceResponse> getSubOrderInvoice(
            @PathVariable Long parentOrderId, @PathVariable Long subOrderId) {
        return ResponseEntity.ok(customerOrderService.getSubOrderInvoice(getCurrentUserId(), parentOrderId, subOrderId));
    }

    @Operation(
        summary = "Download sub-order invoice as PDF",
        description = "Downloads invoice PDF for a single shop's sub-order."
    )
    @GetMapping(value = "/{parentOrderId}/suborders/{subOrderId}/invoice/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadSubOrderInvoicePdf(
            @PathVariable Long parentOrderId, @PathVariable Long subOrderId) {
        CustomerInvoiceResponse invoice = customerOrderService.getSubOrderInvoice(getCurrentUserId(), parentOrderId, subOrderId);
        byte[] pdf = customerInvoicePdfService.generate(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", invoice.getOrderNumber() + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private Long getCurrentUserId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhoneNumber(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.UNAUTHORIZED));
        return user.getId();
    }
}
