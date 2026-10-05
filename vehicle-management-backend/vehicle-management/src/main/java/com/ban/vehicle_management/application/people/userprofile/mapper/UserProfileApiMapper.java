package com.ban.vehicle_management.application.people.userprofile.mapper;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.CreateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.entrypoint.dto.shared.address.request.VietnamAddressRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserProfileApiMapper {

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "addressDetail", ignore = true)
    @Mapping(target = "provinceCode", ignore = true)
    @Mapping(target = "wardCode", ignore = true)
    @Mapping(target = "districtCode", ignore = true)
    @Mapping(target = "addressDisplay", ignore = true)
    @Mapping(target = "structuredAddress", source = "structuredAddress")
    UserProfile toDomain(CreateUserProfileRequest request);

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "addressDetail", ignore = true)
    @Mapping(target = "provinceCode", ignore = true)
    @Mapping(target = "wardCode", ignore = true)
    @Mapping(target = "districtCode", ignore = true)
    @Mapping(target = "addressDisplay", ignore = true)
    @Mapping(target = "structuredAddress", source = "structuredAddress")
    UserProfile toDomain(UpdateUserProfileRequest request);

    UserProfileAdminResponse toAdminResponse(UserProfile userProfile);

    List<UserProfileAdminResponse> toAdminResponses(List<UserProfile> userProfiles);

    default VietnamAddress toDomain(VietnamAddressRequest request) {
        return request == null ? null : new VietnamAddress(
                request.provinceCode(), request.districtCode(), request.wardCode(), request.addressDetail(), null
        );
    }

}
