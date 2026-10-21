package com.team.orderserver.module.order.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void createdCanOnlyGoToPaidOrCancelled() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED));
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    void paidCanShipOrRefund() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.REFUNDED));
        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void terminalStatesCannotTransition() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
            assertFalse(OrderStatus.REFUNDED.canTransitionTo(target));
        }
    }
}
