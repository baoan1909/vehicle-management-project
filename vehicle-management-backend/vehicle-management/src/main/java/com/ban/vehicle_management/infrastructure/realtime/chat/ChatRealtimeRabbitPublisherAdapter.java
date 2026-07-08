package com.ban.vehicle_management.infrastructure.realtime.chat;

import com.ban.vehicle_management.application.operations.chatconversation.model.ChatRealtimeEvent;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatRealtimeRabbitPublisherAdapter implements ChatRealtimeEventPublisherPortOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatRealtimeRabbitPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public ChatRealtimeRabbitPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.chat.realtime.exchange:chat.realtime.exchange}") String exchangeName,
            @Value("${app.chat.realtime.routing-key:chat.realtime}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(ChatRealtimeEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        } catch (AmqpException exception) {
            LOGGER.warn("Failed to publish chat realtime event {}", event, exception);
        }
    }
}
