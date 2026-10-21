package com.team.orderserver.module.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        String status,
        String statusDesc,
        String remark,
        LocalDateTime expireAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) implements Serializable {
}
