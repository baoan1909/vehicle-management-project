package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfilePersistenceMapper {

    UserProfileEntity toEntity(UserProfile domain);

    @Mapping(target = "avatarUrl", ignore = true)
    UserProfile toDomain(UserProfileEntity entity);
}


