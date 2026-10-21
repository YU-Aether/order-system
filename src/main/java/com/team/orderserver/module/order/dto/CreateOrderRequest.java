package com.team.orderserver.module.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "订单商品不能为空")
        @Size(max = 20, message = "单个订单最多 20 种商品")
        @Valid
        List<OrderItemRequest> items,

        @Size(max = 255, message = "备注长度不能超过 255")
        String remark
) implements Serializable {
}
