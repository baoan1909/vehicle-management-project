package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionUserResponse;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ParkingSessionApiMapperUserResponseTest {

    private final ParkingSessionApiMapper mapper = Mappers.getMapper(ParkingSessionApiMapper.class);

    @Test
    void shouldMapUserResponseWithoutInternalFields() {
        ParkingSessionManagementResult.EventResult event = new ParkingSessionManagementResult.EventResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "L1",
                "Lane 1",
                ParkingEventType.CHECK_IN,
                Instant.parse("2026-01-01T08:00:00Z"),
                "51F-12345",
                "/evidence/in.jpg",
                "/evidence/person.jpg",
                UUID.randomUUID(),
                "note"
        );
        ParkingSessionManagementResult result = new ParkingSessionManagementResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CARD-1",
                "UID-1",
                "TYPE",
                "Type name",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "MOTO",
                "Motorbike",
                UUID.randomUUID(),
                "Z1",
                "Zone 1",
                UUID.randomUUID(),
                "LOT",
                "Lot 1",
                "51F-12345",
                "51F-12345",
                Instant.parse("2026-01-01T08:00:00Z"),
                null,
                ParkingSessionStatus.OPEN,
                new BigDecimal("5000"),
                List.of(event)
        );

        ParkingSessionUserResponse response = mapper.toUserResponse(result);

        assertEquals(result.parkingSessionId(), response.getParkingSessionId());
        assertEquals("CARD-1", response.getCardNumber());
        assertEquals("Lot 1", response.getParkingLotName());
        assertEquals(ParkingSessionStatus.OPEN, response.getStatus());
        assertNotNull(response.getLicensePlateInDisplay());
        assertEquals(1, response.getEvents().size());
        assertEquals(ParkingEventType.CHECK_IN, response.getEvents().get(0).getEventType());
        assertEquals("/evidence/in.jpg", response.getEvents().get(0).getLicensePlateImagePath());
        assertNotNull(response.getEvents().get(0).getLicensePlateDetectedDisplay());
    }

    @Test
    void shouldMapUserResponseList() {
        ParkingSessionManagementResult result = new ParkingSessionManagementResult(
                UUID.randomUUID(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                "51F-12345", null, null, null,
                ParkingSessionStatus.CLOSED, BigDecimal.ZERO, List.of()
        );

        List<ParkingSessionUserResponse> responses = mapper.toUserResponses(List.of(result));

        assertEquals(1, responses.size());
        assertNull(responses.get(0).getCheckOutTime());
    }
}
