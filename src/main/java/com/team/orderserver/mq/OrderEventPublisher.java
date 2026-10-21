package com.team.orderserver.mq;

import com.team.orderserver.config.RabbitMQConfig;
import com.team.orderserver.mq.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 订单事件发布者。发布失败不阻断主流程，仅记录告警日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(OrderEvent event) {
        try {
            String routingKey = switch (event.eventType()) {
                case OrderEvent.TYPE_CREATED -> RabbitMQConfig.ROUTING_CREATED;
                case OrderEvent.TYPE_PAID -> RabbitMQConfig.ROUTING_PAID;
                default -> RabbitMQConfig.ROUTING_CANCELLED;
            };
            rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, routingKey, event);
            log.info("订单事件已发布 | type={} | orderNo={}", event.eventType(), event.orderNo());
        } catch (Exception ex) {
            log.error("订单事件发布失败 | type={} | orderNo={}", event.eventType(), event.orderNo(), ex);
        }
    }
}
