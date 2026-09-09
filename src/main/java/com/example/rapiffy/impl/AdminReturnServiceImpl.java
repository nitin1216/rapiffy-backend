package com.example.rapiffy.impl;

import com.example.rapiffy.dto.admin.returns.AdminReviewReturnRequest;
import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;
import com.example.rapiffy.enums.CancelledBy;
import com.example.rapiffy.enums.PaymentMethod;
import com.example.rapiffy.enums.RefundStatus;
import com.example.rapiffy.enums.ReturnStatus;
import com.example.rapiffy.enums.WalletTransactionType;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.impl.customer.CustomerReturnServiceImpl;
import com.example.rapiffy.model.*;
import com.example.rapiffy.model.payment.Payment;
import com.example.rapiffy.model.payment.Refund;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.repos.ShopDeductionRepository;
import com.example.rapiffy.repos.payment.PaymentRepository;
import com.example.rapiffy.repos.payment.RefundRepository;
import com.example.rapiffy.services.AdminReturnService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReturnServiceImpl implements AdminReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ProfileRepository profileRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final ShopDeductionRepository shopDeductionRepository;
    private final RazorpayClient razorpayClient;
    private final CustomerReturnServiceImpl customerReturnService;

    // ─── GET ALL RETURNS FOR SHOP ────────────────────────────────────────────

    @Override
    public List<ReturnRequestResponse> getReturns(Long shopId) {
        return returnRequestRepository.findByOrderShopIdOrderByCreatedAtDesc(shopId)
                .stream().map(customerReturnService::toResponse).toList();
    }

    // ─── APPROVE OR REJECT ───────────────────────────────────────────────────

    @Override
    @Transactional
    public ReturnRequestResponse reviewReturn(Long shopId, Long returnRequestId, AdminReviewReturnRequest request) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ApiException("Return request not found", HttpStatus.NOT_FOUND));

        if (!returnRequest.getOrder().getShop().getId().equals(shopId))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);

        if (returnRequest.getStatus() != ReturnStatus.REQUESTED &&
                returnRequest.getStatus() != ReturnStatus.ADMIN_REVIEWING)
            throw new ApiException("Return request is already " + returnRequest.getStatus(), HttpStatus.BAD_REQUEST);

        returnRequest.setAdminNote(request.getAdminNote());

        if (Boolean.TRUE.equals(request.getApproved())) {
            returnRequest.setStatus(ReturnStatus.APPROVED);
            PaymentMethod paymentMethod = returnRequest.getOrder().getParentOrder().getPaymentMethod();

            if (paymentMethod == PaymentMethod.COD) {
                // COD: customer will receive cash from delivery person at return pickup
                returnRequest.setStatus(ReturnStatus.CASH_REFUND_ON_RETURN);
                recordShopDeduction(returnRequest, "Return approved (COD) — customer will receive cash at return pickup. Amount deducted from your next payout.");
            } else if (paymentMethod == PaymentMethod.WALLET) {
                // Wallet payment: refund back to customer's in-app wallet
                creditWallet(returnRequest);
                returnRequest.setStatus(ReturnStatus.REFUNDED);
                recordShopDeduction(returnRequest, "Return approved (Wallet) — ₹" + returnRequest.getRefundAmount() + " credited back to customer's wallet. Amount deducted from your next payout.");
            } else {
                // Online payment (UPI/Card/Netbanking): refund to original payment source via Razorpay
                initiateRazorpayRefundForReturn(returnRequest);
                returnRequest.setStatus(ReturnStatus.REFUNDED);
                recordShopDeduction(returnRequest, "Return approved — ₹" + returnRequest.getRefundAmount() + " refunded to customer's original payment source. Amount deducted from your next payout.");
            }
        } else {
            if (request.getAdminNote() == null || request.getAdminNote().isBlank())
                throw new ApiException("Admin note is required when rejecting a return", HttpStatus.BAD_REQUEST);
            returnRequest.setStatus(ReturnStatus.REJECTED);
        }

        returnRequestRepository.save(returnRequest);
        return customerReturnService.toResponse(returnRequest);
    }

    // ─── SHOP DEDUCTION RECORD ───────────────────────────────────────────────

    private void recordShopDeduction(ReturnRequest returnRequest, String reason) {
        ShopDeduction deduction = new ShopDeduction();
        deduction.setShop(returnRequest.getOrder().getShop());
        deduction.setReturnRequest(returnRequest);
        deduction.setOrder(returnRequest.getOrder());
        deduction.setAmount(returnRequest.getRefundAmount());
        deduction.setReason(reason);
        deduction.setStatus(com.example.rapiffy.enums.ShopDeductionStatus.PENDING);
        shopDeductionRepository.save(deduction);
        log.info("Shop deduction recorded: ₹{} for shop {} | return request #{}",
                returnRequest.getRefundAmount(),
                returnRequest.getOrder().getShop().getShopName(),
                returnRequest.getId());
    }

    // ─── RAZORPAY REFUND FOR ONLINE PAYMENT RETURNS ──────────────────────────

    private void initiateRazorpayRefundForReturn(ReturnRequest returnRequest) {
        Payment payment = paymentRepository
                .findByParentOrderId(returnRequest.getOrder().getParentOrder().getId())
                .orElseThrow(() -> new ApiException("Payment not found for this order", HttpStatus.NOT_FOUND));

        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", Math.round(returnRequest.getRefundAmount() * 100)); // paise
            refundRequest.put("notes", new JSONObject()
                    .put("reason", "Return approved")
                    .put("return_request_id", returnRequest.getId())
                    .put("sub_order", returnRequest.getOrder().getOrderNumber()));

            com.razorpay.Refund rzpRefund = razorpayClient.payments
                    .refund(payment.getRazorpayPaymentId(), refundRequest);
            String razorpayRefundId = rzpRefund.get("id");

            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setOrder(returnRequest.getOrder());
            refund.setRazorpayRefundId(razorpayRefundId);
            refund.setAmount(returnRequest.getRefundAmount());
            refund.setCancelledBy(CancelledBy.RETURN);
            refund.setReason("Return approved by shop");
            refund.setStatus(RefundStatus.PROCESSING);
            refundRepository.save(refund);

            payment.setRefundedAmount(payment.getRefundedAmount() + returnRequest.getRefundAmount());
            paymentRepository.save(payment);

            log.info("Razorpay refund initiated: ₹{} for return request #{} | sub-order {}",
                    returnRequest.getRefundAmount(), returnRequest.getId(),
                    returnRequest.getOrder().getOrderNumber());

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for return request #{}: {}", returnRequest.getId(), e.getMessage());
            throw new ApiException("Refund initiation failed. Please try again.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── WALLET CREDIT (kept for wallet-based refunds if needed in future) ───

    private void creditWallet(ReturnRequest returnRequest) {
        User customer = returnRequest.getCustomer();
        Wallet wallet = walletRepository.findByUserId(customer.getId()).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUser(customer);
            w.setBalance(0.0);
            return w;
        });
        wallet.setBalance(Math.round((wallet.getBalance() + returnRequest.getRefundAmount()) * 100.0) / 100.0);
        walletRepository.save(wallet);

        WalletTransaction txn = new WalletTransaction();
        txn.setUser(customer);
        txn.setType(WalletTransactionType.CREDIT);
        txn.setSource("RETURN_REFUND");
        txn.setAmount(returnRequest.getRefundAmount());
        txn.setReferenceId(returnRequest.getId());
        txn.setNote("Refund for return request #" + returnRequest.getId()
                + " | Order: " + returnRequest.getOrder().getOrderNumber());
        walletTransactionRepository.save(txn);
    }
}
