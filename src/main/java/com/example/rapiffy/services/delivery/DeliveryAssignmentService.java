package com.example.rapiffy.services.delivery;

import com.example.rapiffy.dto.delivery.AssignDeliveryRequest;
import com.example.rapiffy.dto.delivery.AssignDeliveryResponse;

public interface DeliveryAssignmentService {

    AssignDeliveryResponse assignDeliveryPerson(Long adminUserId, Long orderId, AssignDeliveryRequest request);
}
