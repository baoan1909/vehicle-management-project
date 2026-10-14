package com.ban.vehicle_management.infrastructure.realtime.parking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class ParkingRealtimeRabbitConfigTest {

    private final ParkingRealtimeRabbitConfig config = new ParkingRealtimeRabbitConfig();

    @Test
    void bindingReceivesLotAndZoneScopedRoutingKeys() {
        TopicExchange exchange = config.parkingRealtimeExchange("parking.realtime.exchange");
        Queue queue = config.parkingRealtimeQueue("");

        Binding binding = config.parkingRealtimeBinding(exchange, queue, "parking.realtime");

        assertThat(binding.getRoutingKey()).isEqualTo("parking.realtime.#");
    }
}