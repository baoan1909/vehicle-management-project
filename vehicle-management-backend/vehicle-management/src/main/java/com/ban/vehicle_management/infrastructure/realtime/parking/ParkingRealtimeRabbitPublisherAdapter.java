package com.ban.vehicle_management.infrastructure.realtime.parking;

import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ParkingRealtimeRabbitPublisherAdapter implements ParkingRealtimePublisherPortOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingRealtimeRabbitPublisherAdapter.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;
    private final String routingKey;

    public ParkingRealtimeRabbitPublisherAdapter(
            RabbitTemplate rabbitTemplate,
            @Value("${app.parking.realtime.exchange:parking.realtime.exchange}") String exchangeName,
            @Value("${app.parking.realtime.routing-key:parking.realtime}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(ParkingRealtimeEvent event) {
        if (event == null || event.parkingLotId() == null) {
            LOGGER.warn("Skip invalid parking realtime event {}", event);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKeyFor(event), event);
        } catch (AmqpException exception) {
            Metrics.counter("parking.realtime.publish.failure", "type", String.valueOf(event.eventType()))
                    .increment();
            LOGGER.warn("Failed to publish parking realtime event {} {}", event.eventType(), event.eventId(),
                    exception);
        }
    }

    private String routingKeyFor(ParkingRealtimeEvent event) {
        if (event.zoneId() != null) {
            return routingKey + "." + event.parkingLotId() + "." + event.zoneId();
        }
        return routingKey + "." + event.parkingLotId();
    }
}
