package com.ban.vehicle_management.infrastructure.mapper.iam.account;

import com.ban.vehicle_management.domain.iam.account.model.LoginAttempt;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.LoginAttemptEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoginAttemptPersistenceMapper {

    LoginAttemptEntity toEntity(LoginAttempt domain);

    LoginAttempt toDomain(LoginAttemptEntity entity);
}

