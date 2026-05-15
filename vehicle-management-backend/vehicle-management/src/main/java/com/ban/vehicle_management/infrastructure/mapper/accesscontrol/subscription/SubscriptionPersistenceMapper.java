package com.ban.vehicle_management.infrastructure.mapper.accesscontrol.subscription;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionPersistenceMapper {

    SubscriptionEntity toEntity(Subscription domain);

    Subscription toDomain(SubscriptionEntity entity);
}

