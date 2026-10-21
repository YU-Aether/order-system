package com.team.orderserver.module.order.dto;

import com.team.orderserver.module.payment.entity.PaymentChannel;

import java.io.Serializable;

public record PayOrderRequest(PaymentChannel channel) implements Serializable {
}
