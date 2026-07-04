package com.ban.vehicle_management.application.people.userprofile.mapper;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileSnapshotMapper {

    UserProfile toSnapshot(UserProfile userProfile);
}
