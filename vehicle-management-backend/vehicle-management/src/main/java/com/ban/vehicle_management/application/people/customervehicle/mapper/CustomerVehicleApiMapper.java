package com.ban.vehicle_management.application.people.customervehicle.mapper;

import com.ban.vehicle_management.application.people.customervehicle.model.command.CustomerVehicleBatchCommand;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.CustomerVehicleBatchRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.CreateCustomerVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.UpdateCustomerVehicleBatchRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request.UpdateCustomerVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customervehicle.response.CustomerVehicleAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerVehicleApiMapper {

    default CustomerVehicleBatchCommand toBatchCommand(CustomerVehicleBatchRequest request) {
        if (request == null) {
            return null;
        }
        return new CustomerVehicleBatchCommand(
                request.customerId(),
                request.create() == null ? List.of() : request.create().stream().map(this::toDomain).toList(),
                request.update() == null ? List.of() : request.update().stream().map(this::toDomain).toList(),
                request.inactivate() == null ? List.of() : request.inactivate()
        );
    }

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

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerVehicle toDomain(UpdateCustomerVehicleBatchRequest request);

    CustomerVehicleAdminResponse toAdminResponse(CustomerVehicle customerVehicle);

    List<CustomerVehicleAdminResponse> toAdminResponses(List<CustomerVehicle> customerVehicles);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
