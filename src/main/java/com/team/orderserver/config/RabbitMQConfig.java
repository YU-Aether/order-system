package com.team.orderserver.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：订单事件交换机 + 持久化队列 + 死信队列。
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_EVENT_QUEUE = "order.event.queue";
    public static final String ORDER_DLQ = "order.event.dlq";

    public static final String ROUTING_CREATED = "order.created";
    public static final String ROUTING_PAID = "order.paid";
    public static final String ROUTING_CANCELLED = "order.cancelled";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderEventQueue() {
        return QueueBuilder.durable(ORDER_EVENT_QUEUE)
                .deadLetterExchange(ORDER_EXCHANGE)
                .deadLetterRoutingKey("order.dead")
                .build();
    }

    @Bean
    public Queue orderDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderEventQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEventQueue).to(orderExchange).with(ROUTING_CREATED);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderEventQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEventQueue).to(orderExchange).with(ROUTING_PAID);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderEventQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEventQueue).to(orderExchange).with(ROUTING_CANCELLED);
    }

    @Bean
    public Binding orderDeadLetterBinding(Queue orderDeadLetterQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderDeadLetterQueue).to(orderExchange).with("order.dead");
    }

    /**
     * JSON 序列化，替代默认 JDK 序列化，便于跨语言消费与排查。
     */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
