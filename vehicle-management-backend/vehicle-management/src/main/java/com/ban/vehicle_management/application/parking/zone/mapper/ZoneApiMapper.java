package com.ban.vehicle_management.application.parking.zone.mapper;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.request.CreateZoneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.request.UpdateZoneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.response.ZoneAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ZoneApiMapper {

    @Mapping(target = "zoneId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Zone toDomain(CreateZoneRequest request);

    @Mapping(target = "zoneId", ignore = true)
    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Zone toDomain(UpdateZoneRequest request);

    ZoneAdminResponse toAdminResponse(Zone zone);

    List<ZoneAdminResponse> toAdminResponses(List<Zone> zones);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}