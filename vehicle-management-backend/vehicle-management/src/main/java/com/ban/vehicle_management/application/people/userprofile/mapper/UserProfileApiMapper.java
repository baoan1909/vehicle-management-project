package com.ban.vehicle_management.application.people.userprofile.mapper;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.CreateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.UpdateUserProfileRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.UserProfileAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
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
    UserProfile toDomain(CreateUserProfileRequest request);

    @Mapping(target = "userProfileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    UserProfile toDomain(UpdateUserProfileRequest request);

    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatInstant")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatInstant")
    UserProfileAdminResponse toAdminResponse(UserProfile userProfile);

    List<UserProfileAdminResponse> toAdminResponses(List<UserProfile> userProfiles);

    @Named("formatInstant")
    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant);
    }
}
