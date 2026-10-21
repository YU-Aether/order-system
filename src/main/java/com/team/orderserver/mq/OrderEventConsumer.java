package com.team.orderserver.mq;

import com.team.orderserver.config.RabbitMQConfig;
import com.team.orderserver.mq.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 订单事件消费者：示例中承担通知、积分等下游动作。
 * <p>真实项目中可拆分为独立服务，通过同一交换机订阅。</p>
 */
@Slf4j
@Component
public class OrderEventConsumer {

    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    public void onOrderEvent(OrderEvent event) {
        if (event == null || event.orderNo() == null) {
            log.warn("收到空的订单事件，已忽略");
            return;
        }
        switch (event.eventType()) {
            case OrderEvent.TYPE_CREATED -> log.info("[下游] 订单创建，发送待支付提醒 | orderNo={} | userId={}",
                    event.orderNo(), event.userId());
            case OrderEvent.TYPE_PAID -> log.info("[下游] 订单支付成功，累计用户积分 | orderNo={} | amount={}",
                    event.orderNo(), event.amount());
            default -> log.info("[下游] 订单已取消，释放营销资源 | orderNo={} | reason={}",
                    event.orderNo(), event.reason());
        }
    }
}
