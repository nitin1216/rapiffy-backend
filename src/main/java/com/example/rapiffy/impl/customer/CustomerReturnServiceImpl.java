package com.example.rapiffy.impl.customer;

import com.example.rapiffy.config.RazorpayConfig;
import com.example.rapiffy.dto.customer.returns.*;
import com.example.rapiffy.dto.customer.wallet.*;
import com.example.rapiffy.enums.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.customer.CustomerReturnService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReturnServiceImpl implements CustomerReturnService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnImageRepository returnImageRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;

    // ─── SUBMIT RETURN ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequestResponse submitReturn(Long userId, Long subOrderId, SubmitReturnRequest request) {
        Order subOrder = orderRepository.findById(subOrderId)
                .orElseThrow(() -> new ApiException("Sub-order not found", HttpStatus.NOT_FOUND));

        if (!subOrder.getCustomer().getId().equals(userId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (subOrder.getStatus() != OrderStatus.DELIVERED)
            throw new ApiException("Return can only be raised for delivered orders", HttpStatus.BAD_REQUEST);

        if (returnRequestRepository.existsByOrderId(subOrderId))
            throw new ApiException("Return request already exists for this order", HttpStatus.CONFLICT);

        User customer = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Build return items and calculate total refund
        double totalRefund = 0.0;
        List<ReturnItem> returnItems = new java.util.ArrayList<>();

        for (ReturnItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = orderItemRepository.findById(itemReq.getOrderItemId())
                    .orElseThrow(() -> new ApiException("Order item not found: " + itemReq.getOrderItemId(), HttpStatus.NOT_FOUND));

            // Ensure item belongs to this sub-order
            if (!orderItem.getOrder().getId().equals(subOrderId))
                throw new ApiException("Item " + itemReq.getOrderItemId() + " does not belong to this order", HttpStatus.BAD_REQUEST);

            if (itemReq.getQuantityToReturn() > orderItem.getQuantity())
                throw new ApiException("Return quantity exceeds ordered quantity for: " + orderItem.getProductName(), HttpStatus.BAD_REQUEST);

            double lineRefund = Math.round(orderItem.getSellingPrice() * itemReq.getQuantityToReturn() * 100.0) / 100.0;
            totalRefund += lineRefund;

            ReturnItem returnItem = new ReturnItem();
            returnItem.setOrderItem(orderItem);
            returnItem.setQuantityToReturn(itemReq.getQuantityToReturn());
            returnItem.setLineRefundAmount(lineRefund);
            returnItems.add(returnItem);
        }

        // Save return request
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(subOrder);
        returnRequest.setCustomer(customer);
        returnRequest.setReason(request.getReason());
        returnRequest.setCustomerNote(request.getCustomerNote());
        returnRequest.setRefundAmount(Math.round(totalRefund * 100.0) / 100.0);
        returnRequest.setStatus(ReturnStatus.REQUESTED);
        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        returnItems.forEach(item -> item.setReturnRequest(saved));
        returnItemRepository.saveAll(returnItems);
        saved.setItems(returnItems);

        return toResponse(saved);
    }

    // ─── GET MY RETURNS ──────────────────────────────────────────────────────

    @Override
    public List<ReturnRequestResponse> getMyReturns(Long userId) {
        return returnRequestRepository.findByCustomerIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    // ─── GET WALLET ──────────────────────────────────────────────────────────

    @Override
    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);

        WalletResponse response = new WalletResponse();
        response.setBalance(wallet != null ? wallet.getBalance() : 0.0);
        response.setTransactions(
                walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .stream().map(this::toTransactionResponse).toList()
        );
        return response;
    }

    // ─── WALLET TOP-UP ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public WalletTopupResponse initiateWalletTopup(Long userId, WalletTopupRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", Math.round(request.getAmount() * 100)); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "WALLET-TOPUP-" + userId + "-" + System.currentTimeMillis());
            orderRequest.put("notes", new JSONObject().put("type", "wallet_topup").put("user_id", userId));

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(orderRequest);

            WalletTopupResponse response = new WalletTopupResponse();
            response.setRazorpayOrderId(rzpOrder.get("id"));
            response.setAmount(Math.round(request.getAmount() * 100));
            response.setCurrency("INR");
            response.setRazorpayKeyId(razorpayConfig.getKeyId());
            return response;

        } catch (RazorpayException e) {
            throw new ApiException("Wallet top-up initiation failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public WalletResponse verifyWalletTopup(Long userId, WalletTopupVerifyRequest request) {
        // Verify Razorpay signature
        String generated = generateSignature(
                request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId(),
                razorpayConfig.getKeySecret()
        );
        if (!generated.equals(request.getRazorpaySignature()))
            throw new ApiException("Payment verification failed. Invalid signature.", HttpStatus.BAD_REQUEST);

        // Fetch amount from Razorpay order
        double topupAmount;
        try {
            com.razorpay.Order rzpOrder = razorpayClient.orders.fetch(request.getRazorpayOrderId());
            topupAmount = Math.round(((Number) rzpOrder.get("amount")).doubleValue()) / 100.0;
        } catch (RazorpayException e) {
            throw new ApiException("Could not fetch payment details. Please contact support.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Credit wallet
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(user);
            w.setBalance(0.0);
            return w;
        });
        wallet.setBalance(Math.round((wallet.getBalance() + topupAmount) * 100.0) / 100.0);
        walletRepository.save(wallet);

        // Log transaction
        WalletTransaction txn = new WalletTransaction();
        txn.setUser(user);
        txn.setType(WalletTransactionType.CREDIT);
        txn.setSource("TOPUP");
        txn.setAmount(topupAmount);
        txn.setNote("Wallet top-up via Razorpay | Payment ID: " + request.getRazorpayPaymentId());
        walletTransactionRepository.save(txn);

        return getWallet(userId);
    }

    private String generateSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new ApiException("Signature generation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── RESPONSE BUILDERS ───────────────────────────────────────────────────

    public ReturnRequestResponse toResponse(ReturnRequest r) {
        ReturnRequestResponse res = new ReturnRequestResponse();
        res.setReturnRequestId(r.getId());
        res.setSubOrderId(r.getOrder().getId());
        res.setSubOrderNumber(r.getOrder().getOrderNumber());
        res.setShopName(r.getOrder().getShop().getShopName());
        res.setReason(r.getReason());
        res.setStatus(r.getStatus());
        res.setCustomerNote(r.getCustomerNote());
        res.setAdminNote(r.getAdminNote());
        res.setRefundAmount(r.getRefundAmount());
        res.setRefundMessage(resolveRefundMessage(r));
        res.setCreatedAt(r.getCreatedAt());
        res.setUpdatedAt(r.getUpdatedAt());
        res.setItems(r.getItems().stream().map(this::toItemResponse).toList());
        res.setImageUrls(
                returnImageRepository.findByReturnRequestIdOrderByDisplayOrderAsc(r.getId())
                        .stream().map(ReturnImage::getImageUrl).toList()
        );
        return res;
    }

    private String resolveRefundMessage(ReturnRequest r) {
        if (r.getStatus() == com.example.rapiffy.enums.ReturnStatus.CASH_REFUND_ON_RETURN)
            return "Your return has been approved. You will receive your money (\u20b9" + r.getRefundAmount() + ") in cash from the delivery person at the time of return pickup.";
        if (r.getStatus() == com.example.rapiffy.enums.ReturnStatus.REFUNDED) {
            PaymentMethod paymentMethod = r.getOrder().getParentOrder().getPaymentMethod();
            if (paymentMethod == PaymentMethod.WALLET)
                return "Your refund of \u20b9" + r.getRefundAmount() + " has been credited to your in-app wallet. You can use it on your next order.";
            return "Your refund of \u20b9" + r.getRefundAmount() + " has been initiated to your original payment method. It will reflect within 5-7 business days.";
        }
        if (r.getStatus() == com.example.rapiffy.enums.ReturnStatus.REJECTED)
            return "Your return request has been rejected. Reason: " + (r.getAdminNote() != null ? r.getAdminNote() : "N/A");
        return null;
    }

    private ReturnItemResponse toItemResponse(ReturnItem item) {
        ReturnItemResponse res = new ReturnItemResponse();
        res.setOrderItemId(item.getOrderItem().getId());
        res.setProductName(item.getOrderItem().getProductName());
        res.setImageUrl(item.getOrderItem().getImageUrl());
        res.setSellingPrice(item.getOrderItem().getSellingPrice());
        res.setQuantityToReturn(item.getQuantityToReturn());
        res.setLineRefundAmount(item.getLineRefundAmount());
        return res;
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction txn) {
        WalletTransactionResponse res = new WalletTransactionResponse();
        res.setTransactionId(txn.getId());
        res.setType(txn.getType());
        res.setSource(txn.getSource());
        res.setAmount(txn.getAmount());
        res.setNote(txn.getNote());
        res.setCreatedAt(txn.getCreatedAt());
        return res;
    }
}
