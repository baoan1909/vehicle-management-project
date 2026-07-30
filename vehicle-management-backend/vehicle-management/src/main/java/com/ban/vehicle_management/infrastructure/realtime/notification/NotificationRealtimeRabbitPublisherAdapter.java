package com.ban.vehicle_management.infrastructure.realtime.notification;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationRealtimeRabbitPublisherAdapter implements NotificationRealtimeEventPublisherPortOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRealtimeRabbitPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public NotificationRealtimeRabbitPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.notification.realtime.exchange:notification.realtime.exchange}") String exchangeName,
            @Value("${app.notification.realtime.routing-key:notification.realtime}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(NotificationRealtimeMessage message) {
        if (message == null || message.accountId() == null) {
            LOGGER.warn("Skip invalid notification realtime message {}", message);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
        } catch (AmqpException exception) {
            LOGGER.warn("Failed to publish notification realtime message {}", message, exception);
        }
    }
}
