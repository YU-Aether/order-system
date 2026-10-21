package com.team.orderserver.module.order.entity;

/**
 * 订单状态机定义。
 * <pre>
 * CREATED ──▶ PAID ──▶ SHIPPED ──▶ COMPLETED
 *    │          │          │
 *    └──▶ CANCELLED       └──▶ REFUNDED
 * </pre>
 */
public enum OrderStatus {

    CREATED("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == REFUNDED;
            case SHIPPED -> target == COMPLETED || target == REFUNDED;
            case COMPLETED -> target == REFUNDED;
            case CANCELLED, REFUNDED -> false;
        };
    }
}
