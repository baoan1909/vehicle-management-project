package com.ban.vehicle_management.application.catalog.vehicletype.mapper;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.CreateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.UpdateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response.VehicleTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleTypeApiMapper {

    @Mapping(target = "vehicleTypeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    VehicleType toDomain(CreateVehicleTypeRequest request);

    @Mapping(target = "vehicleTypeId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    VehicleType toDomain(UpdateVehicleTypeRequest request);

    VehicleTypeAdminResponse toAdminResponse(VehicleType vehicleType);

    List<VehicleTypeAdminResponse> toAdminResponses(List<VehicleType> vehicleTypes);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}

