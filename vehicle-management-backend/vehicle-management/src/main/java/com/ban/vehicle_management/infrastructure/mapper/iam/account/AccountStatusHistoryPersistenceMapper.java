package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.AccountStatusHistory;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountStatusHistoryPersistenceMapper {

    AccountStatusHistoryEntity toEntity(AccountStatusHistory domain);

    AccountStatusHistory toDomain(AccountStatusHistoryEntity entity);
}

