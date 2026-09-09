package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.cancellation.AdminReviewCancellationRequest;
import com.example.rapiffy.dto.customer.cancellation.CancellationRequestResponse;

import java.util.List;

public interface AdminCancellationService {

    List<CancellationRequestResponse> getCancellations(Long shopId);

    CancellationRequestResponse reviewCancellation(Long shopId, Long cancellationRequestId, AdminReviewCancellationRequest request);
}
