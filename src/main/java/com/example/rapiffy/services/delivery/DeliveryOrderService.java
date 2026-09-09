package com.example.rapiffy.services.delivery;

import com.example.rapiffy.dto.delivery.DeliveryRouteResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderDetailResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderStatsResponse;
import com.example.rapiffy.dto.delivery.DeliveryOrderSummaryResponse;
import com.example.rapiffy.dto.delivery.UpdateDeliveryStatusRequest;
import com.example.rapiffy.enums.OrderStatus;

import java.util.List;

public interface DeliveryOrderService {

    List<DeliveryOrderSummaryResponse> getMyOrders(Long deliveryUserId, OrderStatus status);

    DeliveryOrderDetailResponse getOrderDetail(Long deliveryUserId, Long orderId);

    DeliveryOrderDetailResponse updateOrderStatus(Long deliveryUserId, Long orderId, UpdateDeliveryStatusRequest request);

    DeliveryOrderStatsResponse getMyStats(Long deliveryUserId);

    DeliveryRouteResponse getRoute(Long deliveryUserId, Long orderId);
}
