package com.ban.vehicle_management.entrypoint.converter;

import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckOutParkingSessionRequest;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionRequestConverter {

    private final ObjectMapper objectMapper;

    public ParkingSessionRequestConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CheckInParkingSessionRequest toCheckInRequest(String json) {
        return read(json, CheckInParkingSessionRequest.class);
    }

    public CheckOutParkingSessionRequest toCheckOutRequest(String json) {
        return read(json, CheckOutParkingSessionRequest.class);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("request part must be valid JSON");
        }
    }
}
