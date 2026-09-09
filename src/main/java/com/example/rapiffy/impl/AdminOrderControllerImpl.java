package com.example.rapiffy.impl;

import com.example.rapiffy.dto.admin.DeliveryPersonResponse;
import com.example.rapiffy.dto.delivery.AssignDeliveryRequest;
import com.example.rapiffy.dto.delivery.AssignDeliveryResponse;
import com.example.rapiffy.dto.invoice.InvoiceResponse;
import com.example.rapiffy.controller.AdminOrderController;
import com.example.rapiffy.dto.order.OrderDetailResponse;
import com.example.rapiffy.dto.order.UpdateOrderStatusRequest;
import com.example.rapiffy.dto.order.OrderSummaryResponse;
import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminOrderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AdminOrderControllerImpl implements AdminOrderController {

    private final AdminOrderService orderService;
    private final UserRepository userRepository;
    private final InvoicePdfService invoicePdfService;
    private final com.example.rapiffy.services.delivery.DeliveryAssignmentService deliveryAssignmentService;

    public AdminOrderControllerImpl(AdminOrderService orderService,
                                    UserRepository userRepository,
                                    InvoicePdfService invoicePdfService,
                                    com.example.rapiffy.services.delivery.DeliveryAssignmentService deliveryAssignmentService) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.invoicePdfService = invoicePdfService;
        this.deliveryAssignmentService = deliveryAssignmentService;
    }

    @Override
    public ResponseEntity<AssignDeliveryResponse> assignDeliveryPerson(Long orderId, AssignDeliveryRequest request) {
        return ResponseEntity.ok(deliveryAssignmentService.assignDeliveryPerson(getCurrentUserId(), orderId, request));
    }

    @Override
    public ResponseEntity<List<DeliveryPersonResponse>> getDeliveryPersons() {
        return ResponseEntity.ok(orderService.getDeliveryPersons(getCurrentUserId()));
    }

    @Override
    public ResponseEntity<List<OrderSummaryResponse>> getOrders(OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrders(getCurrentUserId(), status));
    }

    @Override
    public ResponseEntity<OrderDetailResponse> getOrderDetail(Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetail(getCurrentUserId(), orderId));
    }

    @Override
    public ResponseEntity<List<OrderStatus>> getOrderStatuses() {
        return ResponseEntity.ok(List.of(
            OrderStatus.CONFIRMED,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED,
            OrderStatus.REJECTED
        ));
    }

    @Override
    public ResponseEntity<OrderDetailResponse> updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(getCurrentUserId(), orderId, request.getStatus());
    }

    @Override
    public ResponseEntity<InvoiceResponse> getInvoice(Long orderId) {
        return ResponseEntity.ok(orderService.getInvoice(getCurrentUserId(), orderId));
    }

    @Override
    public ResponseEntity<byte[]> downloadInvoicePdf(Long orderId) {
        InvoiceResponse invoice = orderService.getInvoice(getCurrentUserId(), orderId);
        byte[] pdf = invoicePdfService.generate(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", invoice.getInvoiceId() + ".pdf");
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
