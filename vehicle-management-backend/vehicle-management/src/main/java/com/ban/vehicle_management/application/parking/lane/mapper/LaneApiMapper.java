package com.ban.vehicle_management.application.parking.lane.mapper;

import com.ban.vehicle_management.domain.parking.lane.model.Lane;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.request.CreateLaneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.request.UpdateLaneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.lane.response.LaneResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LaneApiMapper {

    @Mapping(target = "laneId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Lane toDomain(CreateLaneRequest request);

    @Mapping(target = "laneId", ignore = true)
    @Mapping(target = "gateId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Lane toDomain(UpdateLaneRequest request);

    LaneResponse toAdminResponse(Lane lane);

    List<LaneResponse> toAdminResponses(List<Lane> lanes);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}