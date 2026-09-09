package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.returns.AdminReviewReturnRequest;
import com.example.rapiffy.dto.customer.returns.ReturnRequestResponse;

import java.util.List;

public interface AdminReturnService {

    // All return requests for this admin's shop
    List<ReturnRequestResponse> getReturns(Long shopId);

    // Approve or reject a return request
    ReturnRequestResponse reviewReturn(Long shopId, Long returnRequestId, AdminReviewReturnRequest request);
}
