package com.team.orderserver.module.order.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指定日期的营收统计。
 *
 * @param date            统计日期
 * @param paidOrderCount  当日支付订单数
 * @param paidAmount      当日支付总额
 * @param refundOrderCount 当日退款订单数
 * @param refundAmount    当日退款总额
 * @param netAmount       净收入 = 支付总额 - 退款总额
 * @param createdOrderCount 当日新建订单数
 */
public record RevenueResponse(
        String date,
        long paidOrderCount,
        BigDecimal paidAmount,
        long refundOrderCount,
        BigDecimal refundAmount,
        BigDecimal netAmount,
        long createdOrderCount
) implements Serializable {
}
