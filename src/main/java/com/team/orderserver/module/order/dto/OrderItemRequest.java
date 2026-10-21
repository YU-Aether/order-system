package com.team.orderserver.module.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record OrderItemRequest(
        @NotNull(message = "商品 ID 不能为空") Long productId,
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1")
        @Max(value = 999, message = "单个商品最多购买 999 件")
        Integer quantity
) implements Serializable {
}
