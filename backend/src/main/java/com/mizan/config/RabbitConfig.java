package com.mizan.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Exchanges
    public static final String AUDIT_EXCHANGE = "mizan.audit";
    public static final String PRICE_EXCHANGE = "mizan.price";
    public static final String NOTIFICATION_EXCHANGE = "mizan.notification";

    // Queues
    public static final String AUDIT_QUEUE = "mizan.audit.log";
    public static final String PRICE_UPDATE_QUEUE = "mizan.price.update";
    public static final String NOTIFICATION_QUEUE = "mizan.notification.push";

    // Routing keys
    public static final String AUDIT_ROUTING_KEY = "audit.#";
    public static final String PRICE_ROUTING_KEY = "price.update";

    // ── Exchanges ──
    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(AUDIT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange priceExchange() {
        return new DirectExchange(PRICE_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange notificationExchange() {
        return new FanoutExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    // ── Queues ──
    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(AUDIT_QUEUE)
                .withArgument("x-message-ttl", 604800000L) // 7 days
                .build();
    }

    @Bean
    public Queue priceUpdateQueue() {
        return QueueBuilder.durable(PRICE_UPDATE_QUEUE)
                .withArgument("x-message-ttl", 300000L) // 5 minutes
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    // ── Bindings ──
    @Bean
    public Binding auditBinding(Queue auditQueue, TopicExchange auditExchange) {
        return BindingBuilder.bind(auditQueue).to(auditExchange).with(AUDIT_ROUTING_KEY);
    }

    @Bean
    public Binding priceBinding(Queue priceUpdateQueue, DirectExchange priceExchange) {
        return BindingBuilder.bind(priceUpdateQueue).to(priceExchange).with(PRICE_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, FanoutExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange);
    }

    // ── Serialization ──
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
