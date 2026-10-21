package com.team.orderserver.module.order.entity;

/**
 * 退款申请状态。与订单状态解耦：申请退款不改变订单状态，审批通过后才流转为 REFUNDED。
 */
public enum RefundStatus {

    /** 未申请 */
    NONE,
    /** 已申请，等待管理员处理 */
    PENDING,
    /** 管理员同意，订单已退款 */
    APPROVED,
    /** 管理员拒绝 */
    REJECTED
}
