package com.ban.vehicle_management.application.operations.chatconversation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionChatOrder;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.SubscriptionOrderSnapshotJsonMapper;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionOrderSnapshotJsonMapperTest {

    @Test
    void serializesImmutableOrderSnapshotWithJavaTimeValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SubscriptionOrderSnapshotJsonMapper mapper = new SubscriptionOrderSnapshotJsonMapper(objectMapper);
        UUID subscriptionId = UUID.fromString("6663bdeb-03e8-4c30-9899-a9705cf29fd4");
        Instant sharedAt = Instant.parse("2026-11-19T01:29:38Z");
        SubscriptionChatOrder order = new SubscriptionChatOrder(
                subscriptionId,
                UUID.fromString("f2b20875-81e0-4cd9-8dc8-3ab888bda8c5"),
                "HCMUTE",
                "Bãi xe HCMUTE",
                "MONTHLY",
                "Vé tháng",
                "123456H",
                "Xe máy",
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 12, 3),
                new BigDecimal("80000"),
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-11-04T00:27:59Z")
        );

        JsonNode payload = objectMapper.readTree(mapper.toJson(order, sharedAt));

        assertEquals(1, payload.path("schemaVersion").asInt());
        assertEquals(subscriptionId.toString(), payload.path("subscriptionId").asText());
        assertEquals("DH-6663BDEB", payload.path("orderCode").asText());
        assertEquals("2026-11-04", payload.path("effectiveFrom").asText());
        assertEquals("ACTIVE", payload.path("status").asText());
        assertEquals(sharedAt.toString(), payload.path("sharedAt").asText());
    }
}
