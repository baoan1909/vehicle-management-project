package com.ban.vehicle_management.infrastructure.realtime.parking;

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
public class ParkingRealtimeRabbitConfig {

    @Bean
    public TopicExchange parkingRealtimeExchange(
            @Value("${app.parking.realtime.exchange:parking.realtime.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue parkingRealtimeQueue(
            @Value("${app.parking.realtime.queue:}") String queueName
    ) {
        if (StringUtils.hasText(queueName)) {
            return new Queue(queueName, true);
        }
        return new AnonymousQueue();
    }

    @Bean
    public Binding parkingRealtimeBinding(
            TopicExchange parkingRealtimeExchange,
            Queue parkingRealtimeQueue,
            @Value("${app.parking.realtime.routing-key:parking.realtime}") String routingKey
    ) {
        return BindingBuilder.bind(parkingRealtimeQueue)
                .to(parkingRealtimeExchange)
                .with(routingKey + ".#");
    }
}
