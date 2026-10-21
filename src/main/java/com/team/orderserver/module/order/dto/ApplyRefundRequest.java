package com.team.orderserver.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 用户提交退款申请。
 */
public record ApplyRefundRequest(
        @NotBlank(message = "请填写退款理由")
        @Size(max = 255, message = "退款理由不能超过 255 字")
        String reason
) implements Serializable {
}
