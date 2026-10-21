package com.team.orderserver.mq.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单领域事件，通过 RabbitMQ 异步广播。
 */
public record OrderEvent(
        String eventId,
        String eventType,
        String orderNo,
        Long userId,
        BigDecimal amount,
        String reason,
        LocalDateTime occurredAt
) implements Serializable {

    public static final String TYPE_CREATED = "ORDER_CREATED";
    public static final String TYPE_PAID = "ORDER_PAID";
    public static final String TYPE_CANCELLED = "ORDER_CANCELLED";

    public static OrderEvent created(String orderNo, Long userId, BigDecimal amount) {
        return new OrderEvent(null, TYPE_CREATED, orderNo, userId, amount, null, LocalDateTime.now());
    }

    public static OrderEvent paid(String orderNo, Long userId, BigDecimal amount) {
        return new OrderEvent(null, TYPE_PAID, orderNo, userId, amount, null, LocalDateTime.now());
    }

    public static OrderEvent cancelled(String orderNo, Long userId, String reason) {
        return new OrderEvent(null, TYPE_CANCELLED, orderNo, userId, null, reason, LocalDateTime.now());
    }
}
