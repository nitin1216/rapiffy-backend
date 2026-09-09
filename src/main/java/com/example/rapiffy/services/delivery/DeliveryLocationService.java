package com.example.rapiffy.services.delivery;

import com.example.rapiffy.dto.delivery.DeliveryLocationResponse;
import com.example.rapiffy.dto.delivery.PushLocationRequest;

public interface DeliveryLocationService {

    // Called by delivery person app — pushes live GPS to backend
    void pushLocation(Long deliveryUserId, PushLocationRequest request);

    // Called by customer app — polls latest delivery person position
    DeliveryLocationResponse getDeliveryLocation(Long customerId, Long orderId);
}
