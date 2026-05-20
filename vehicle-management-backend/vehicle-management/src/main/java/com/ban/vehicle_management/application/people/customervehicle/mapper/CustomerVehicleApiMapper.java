package com.ban.vehicle_management.application.people.customervehicle.mapper;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.CreateCustomerVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.UpdateCustomerVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response.CustomerVehicleAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerVehicleApiMapper {

    @Mapping(target = "customerVehicleId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerVehicle toDomain(CreateCustomerVehicleRequest request);

    @Mapping(target = "customerVehicleId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerVehicle toDomain(UpdateCustomerVehicleRequest request);

    CustomerVehicleAdminResponse toAdminResponse(CustomerVehicle customerVehicle);

    List<CustomerVehicleAdminResponse> toAdminResponses(List<CustomerVehicle> customerVehicles);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
