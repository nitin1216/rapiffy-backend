package com.example.rapiffy.dto.order;

import com.example.rapiffy.enums.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    private OrderStatus status;
}
