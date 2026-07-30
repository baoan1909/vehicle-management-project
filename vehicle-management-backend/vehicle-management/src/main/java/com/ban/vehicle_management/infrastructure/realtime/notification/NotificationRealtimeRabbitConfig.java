package com.ban.vehicle_management.infrastructure.realtime.notification;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class NotificationRealtimeRabbitConfig {

    @Bean
    public TopicExchange notificationRealtimeExchange(
            @Value("${app.notification.realtime.exchange:notification.realtime.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue notificationRealtimeQueue(
            @Value("${app.notification.realtime.queue:}") String queueName
    ) {
        if (StringUtils.hasText(queueName)) {
            return new Queue(queueName, true);
        }
        return new AnonymousQueue();
    }

    @Bean
    public Binding notificationRealtimeBinding(
            TopicExchange notificationRealtimeExchange,
            Queue notificationRealtimeQueue,
            @Value("${app.notification.realtime.routing-key:notification.realtime}") String routingKey
    ) {
        return BindingBuilder.bind(notificationRealtimeQueue).to(notificationRealtimeExchange).with(routingKey);
    }
}
