package com.ban.vehicle_management.infrastructure.realtime.parking;

import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.rabbitmq.listener-enabled", havingValue = "true", matchIfMissing = true)
public class ParkingRealtimeRabbitConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingRealtimeRabbitConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;

    public ParkingRealtimeRabbitConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = "#{parkingRealtimeQueue.name}")
    public void handle(ParkingRealtimeEvent event) {
        if (event == null || event.parkingLotId() == null) {
            LOGGER.warn("Skip invalid parking realtime event {}", event);
            return;
        }
        try {
            messagingTemplate.convertAndSend("/topic/parking-lots/" + event.parkingLotId(), event);
        } catch (RuntimeException exception) {
            Metrics.counter("parking.realtime.forward.failure").increment();
            LOGGER.warn("Failed to forward parking realtime event {} {}", event.eventType(), event.eventId(),
                    exception);
        }
    }
}
