package com.team.orderserver.module.product.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 库存调整：delta 为正表示补货，为负表示扣减。
 */
public record StockAdjustRequest(
        @NotNull(message = "调整数量不能为空") Integer delta,
        String reason
) implements Serializable {
}
