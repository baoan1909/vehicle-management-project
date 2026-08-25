package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountIdentityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountIdentityPersistenceMapper {

    @Mapping(target = "account", ignore = true)
    AccountIdentityEntity toEntity(AccountIdentity domain);

    AccountIdentity toDomain(AccountIdentityEntity entity);
}
