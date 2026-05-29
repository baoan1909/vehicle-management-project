package com.ban.vehicle_management.application.parking.gate.mapper;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.request.CreateGateRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.request.UpdateGateRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.response.GateAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GateApiMapper {

    @Mapping(target = "gateId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Gate toDomain(CreateGateRequest request);

    @Mapping(target = "gateId", ignore = true)
    @Mapping(target = "zoneId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Gate toDomain(UpdateGateRequest request);

    GateAdminResponse toAdminResponse(Gate gate);

    List<GateAdminResponse> toAdminResponses(List<Gate> gates);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}