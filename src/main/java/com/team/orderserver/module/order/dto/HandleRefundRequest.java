package com.team.orderserver.module.order.dto;

import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 管理员处理退款申请（拒绝时填写说明）。
 */
public record HandleRefundRequest(
        @Size(max = 255, message = "处理备注不能超过 255 字")
        String remark
) implements Serializable {
}
