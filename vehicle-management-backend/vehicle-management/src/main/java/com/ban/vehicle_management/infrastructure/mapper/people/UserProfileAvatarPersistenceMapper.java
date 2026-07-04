package com.ban.vehicle_management.infrastructure.mapper.people;

import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.people.UserProfileAvatarEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileAvatarPersistenceMapper {

    UserProfileAvatarEntity toEntity(UserProfileAvatar domain);

    UserProfileAvatar toDomain(UserProfileAvatarEntity entity);
}
