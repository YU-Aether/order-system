package com.team.orderserver.module.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal productPrice,
        Integer quantity,
        BigDecimal subtotal
) implements Serializable {
}
