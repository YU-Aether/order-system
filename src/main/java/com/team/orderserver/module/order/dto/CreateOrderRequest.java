package com.team.orderserver.module.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "订单商品不能为空")
        @Size(max = 20, message = "单个订单最多 20 种商品")
        @Valid
        List<OrderItemRequest> items,

        @NotBlank(message = "收货人不能为空")
        @Size(max = 50, message = "收货人姓名不能超过 50 字")
        String receiverName,

        @NotBlank(message = "联系电话不能为空")
        @Pattern(regexp = "^[0-9+\\-]{6,20}$", message = "联系电话格式不正确")
        String receiverPhone,

        @NotBlank(message = "收货地址不能为空")
        @Size(max = 255, message = "收货地址不能超过 255 字")
        String receiverAddress,

        @Size(max = 255, message = "备注长度不能超过 255")
        String remark
) implements Serializable {
}
