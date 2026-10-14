package com.ban.vehicle_management.application.parking.parkinglayout.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Bridges Spring MVC's Jackson 3 boundary values with the Jackson 2 JSON tree
 * retained by the domain and Hibernate JSONB mapping.
 */
@Component
public class ParkingLayoutJsonValueMapper {

    private final ObjectMapper objectMapper;

    public ParkingLayoutJsonValueMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Named("toLegacyJsonNode")
    public JsonNode toLegacyJsonNode(Object value) {
        return value == null ? null : objectMapper.valueToTree(value);
    }

    @Named("toHttpJsonValue")
    public Object toHttpJsonValue(JsonNode value) {
        return value == null ? null : objectMapper.convertValue(value, Object.class);
    }
}
