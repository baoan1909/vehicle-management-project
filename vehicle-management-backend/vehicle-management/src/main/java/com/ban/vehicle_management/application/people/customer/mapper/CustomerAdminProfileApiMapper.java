package com.ban.vehicle_management.application.people.customer.mapper;

import com.ban.vehicle_management.application.people.customer.model.command.CreateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminVehicleDiffCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CreateCustomerAdminProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CreateCustomerAdminRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.CreateCustomerAdminVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminVehicleRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminVehicleOperationsRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminProfileResponse;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.CreateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;
import com.ban.vehicle_management.application.people.customervehicle.mapper.CustomerVehicleApiMapper;
import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserProfileApiMapper.class, CustomerApiMapper.class, CustomerVehicleApiMapper.class})
public interface CustomerAdminProfileApiMapper {

    default CreateCustomerAdminProfileCommand toCreateCommand(CreateCustomerAdminProfileRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateCustomerAdminProfileCommand(
                request.userProfile() == null ? null : toDomain(request.userProfile()),
                request.customer() == null ? null : toDomain(request.customer()),
                mapCreateVehicles(request.customerVehicles())
        );
    }

    default UpdateCustomerAdminProfileCommand toUpdateCommand(UpdateCustomerAdminProfileRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateCustomerAdminProfileCommand(
                request.userProfile() == null ? null : toDomain(request.userProfile()),
                request.customer() == null ? null : toDomain(request.customer()),
                toVehicleDiffCommand(request.vehicles())
        );
    }

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserProfile toDomain(CreateUserProfileRequest request);

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserProfile toDomain(UpdateUserProfileRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toDomain(CreateCustomerAdminRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toDomain(UpdateCustomerAdminRequest request);

    @Mapping(target = "customerVehicleId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerVehicle toDomain(CreateCustomerAdminVehicleRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    CustomerVehicle toDomain(UpdateCustomerAdminVehicleRequest request);

    CustomerAdminProfileResponse toResponse(CustomerAdminProfileResult result);

    default UpdateCustomerAdminVehicleDiffCommand toVehicleDiffCommand(UpdateCustomerAdminVehicleOperationsRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateCustomerAdminVehicleDiffCommand(
                mapCreateVehicles(request.create()),
                mapUpdateVehicles(request.update()),
                request.inactivate() == null ? List.of() : request.inactivate()
        );
    }

    default List<CustomerVehicle> mapCreateVehicles(List<CreateCustomerAdminVehicleRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(this::toDomain).toList();
    }

    default List<CustomerVehicle> mapUpdateVehicles(List<UpdateCustomerAdminVehicleRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(this::toDomain).toList();
    }
}
