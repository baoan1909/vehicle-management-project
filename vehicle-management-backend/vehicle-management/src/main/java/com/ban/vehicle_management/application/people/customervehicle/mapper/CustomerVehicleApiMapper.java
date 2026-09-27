package com.ban.vehicle_management.application.people.customervehicle.mapper;

import com.ban.vehicle_management.application.people.customervehicle.model.command.CustomerVehicleBatchCommand;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;
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
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

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

    @Mapping(target = "licensePlateNormalized", ignore = true)
    @Mapping(target = "licensePlateDisplay", ignore = true)
    @Mapping(target = "vehicleIdentifier", ignore = true)
    @Mapping(target = "plateFormat", ignore = true)
    @Mapping(target = "validFormat", ignore = true)
    @Mapping(target = "needsReview", ignore = true)
    CustomerVehicleAdminResponse toAdminResponse(CustomerVehicle customerVehicle);

    List<CustomerVehicleAdminResponse> toAdminResponses(List<CustomerVehicle> customerVehicles);

    @AfterMapping
    default void addLicensePlateView(CustomerVehicle source, @MappingTarget CustomerVehicleAdminResponse target) {
        LicensePlateResolution resolution = new LicensePlatePolicy().resolve(
                source.getLicensePlate(),
                source.getLicensePlate() == null ? "BICYCLE" : null
        );
        target.setLicensePlateNormalized(resolution.normalized());
        target.setLicensePlateDisplay(resolution.display());
        target.setVehicleIdentifier(source.getLicensePlate() == null && source.getCustomerVehicleId() != null
                ? "BICYCLE-" + source.getCustomerVehicleId().toString().substring(0, 8).toUpperCase()
                : null);
        target.setPlateFormat(resolution.format().name());
        target.setValidFormat(resolution.validFormat());
        target.setNeedsReview(resolution.needsReview());
    }

}
