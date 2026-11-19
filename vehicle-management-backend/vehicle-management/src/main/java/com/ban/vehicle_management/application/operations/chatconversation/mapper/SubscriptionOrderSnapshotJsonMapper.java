package com.ban.vehicle_management.application.operations.chatconversation.mapper;

import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionChatOrder;
import com.ban.vehicle_management.application.operations.chatconversation.model.SubscriptionOrderSnapshotPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionOrderSnapshotJsonMapper {

    private final ObjectMapper objectMapper;

    public SubscriptionOrderSnapshotJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(SubscriptionChatOrder order, Instant sharedAt) {
        SubscriptionOrderSnapshotPayload payload = new SubscriptionOrderSnapshotPayload(
                1,
                order.subscriptionId(),
                "DH-" + order.subscriptionId().toString().substring(0, 8).toUpperCase(),
                order.parkingLotId(),
                order.parkingLotCode(),
                order.parkingLotName(),
                order.ticketTypeCode(),
                order.ticketTypeName(),
                order.licensePlate(),
                order.vehicleTypeName(),
                toIsoText(order.effectiveFrom()),
                toIsoText(order.effectiveTo()),
                order.price(),
                order.status(),
                toIsoText(order.createdAt()),
                toIsoText(sharedAt)
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Subscription order snapshot could not be serialized", exception);
        }
    }

    private String toIsoText(TemporalAccessor temporalValue) {
        return temporalValue == null ? null : temporalValue.toString();
    }
}
