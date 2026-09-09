package com.example.rapiffy.services;

import com.example.rapiffy.dto.admin.DeliveryPersonResponse;
import com.example.rapiffy.dto.delivery.OnboardDeliveryPersonRequest;

import java.util.List;

public interface DeliveryPersonService {
    DeliveryPersonResponse onboardByAdmin(Long adminUserId, OnboardDeliveryPersonRequest request);
    DeliveryPersonResponse onboardBySuperAdmin(Long shopProfileId, OnboardDeliveryPersonRequest request);
    List<DeliveryPersonResponse> getDeliveryPersonsByShop(Long shopProfileId);
    DeliveryPersonResponse deactivate(Long deliveryPersonId);
}
