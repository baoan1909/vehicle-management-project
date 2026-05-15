package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountPersistenceMapper {

    AccountEntity toEntity(Account domain);

    Account toDomain(AccountEntity entity);
}

