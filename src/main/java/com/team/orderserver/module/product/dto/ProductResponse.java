package com.team.orderserver.module.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        Integer stock,
        String status,
        LocalDateTime createdAt
) implements Serializable {
}
