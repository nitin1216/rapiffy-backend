package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;
import com.example.rapiffy.dto.customer.returns.SubmitReturnRequest;
import com.example.rapiffy.dto.customer.wallet.WalletResponse;
import com.example.rapiffy.dto.customer.wallet.WalletTopupRequest;
import com.example.rapiffy.dto.customer.wallet.WalletTopupResponse;
import com.example.rapiffy.dto.customer.wallet.WalletTopupVerifyRequest;

import java.util.List;

public interface CustomerReturnService {

    // Submit a return request for a delivered sub-order
    ReturnRequestResponse submitReturn(Long userId, Long subOrderId, SubmitReturnRequest request);

    // Get all return requests by the customer
    List<ReturnRequestResponse> getMyReturns(Long userId);

    // Get wallet balance + transaction history
    WalletResponse getWallet(Long userId);

    // Initiate wallet top-up — creates a Razorpay order for the given amount
    WalletTopupResponse initiateWalletTopup(Long userId, WalletTopupRequest request);

    // Verify wallet top-up — validates signature and credits wallet
    WalletResponse verifyWalletTopup(Long userId, WalletTopupVerifyRequest request);
}
