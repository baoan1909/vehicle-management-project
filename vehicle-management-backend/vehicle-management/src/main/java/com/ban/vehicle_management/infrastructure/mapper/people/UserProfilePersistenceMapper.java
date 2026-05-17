package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfilePersistenceMapper {

    UserProfileEntity toEntity(UserProfile domain);

    UserProfile toDomain(UserProfileEntity entity);
}


