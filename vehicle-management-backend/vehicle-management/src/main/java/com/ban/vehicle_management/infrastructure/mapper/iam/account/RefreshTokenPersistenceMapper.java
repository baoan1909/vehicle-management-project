package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.RefreshToken;
import com.ban.vehicle_management.infrastructure.persistence.iam.account.RefreshTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RefreshTokenPersistenceMapper {

    RefreshTokenEntity toEntity(RefreshToken domain);

    RefreshToken toDomain(RefreshTokenEntity entity);
}
