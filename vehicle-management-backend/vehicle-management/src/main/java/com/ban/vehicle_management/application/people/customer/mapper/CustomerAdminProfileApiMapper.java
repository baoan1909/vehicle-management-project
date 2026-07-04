package com.ban.vehicle_management.application.people.customer.mapper;

import com.ban.vehicle_management.application.people.customer.model.command.UpdateCustomerAdminProfileCommand;
import com.ban.vehicle_management.application.people.customer.model.result.CustomerAdminProfileResult;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.request.UpdateCustomerAdminRequest;
import com.ban.vehicle_management.entrypoint.dto.people.customer.response.CustomerAdminProfileResponse;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;
import com.ban.vehicle_management.application.people.customervehicle.mapper.CustomerVehicleApiMapper;
import com.ban.vehicle_management.application.people.userprofile.mapper.UserProfileApiMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserProfileApiMapper.class, CustomerApiMapper.class, CustomerVehicleApiMapper.class})
public interface CustomerAdminProfileApiMapper {

    default UpdateCustomerAdminProfileCommand toUpdateCommand(UpdateCustomerAdminProfileRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateCustomerAdminProfileCommand(
                request.userProfile() == null ? null : toDomain(request.userProfile()),
                request.customer() == null ? null : toDomain(request.customer())
        );
    }

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    UserProfile toDomain(UpdateUserProfileRequest request);

    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "customerCode", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "accountEmail", ignore = true)
    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer toDomain(UpdateCustomerAdminRequest request);

    CustomerAdminProfileResponse toResponse(CustomerAdminProfileResult result);
}
