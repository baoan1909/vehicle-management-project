package com.ban.vehicle_management.infrastructure.mapper.accesscontrol;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.SubscriptionRenewal;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionRenewalEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SubscriptionRenewalPersistenceMapper {
    SubscriptionRenewalEntity toEntity(SubscriptionRenewal domain);
    SubscriptionRenewal toDomain(SubscriptionRenewalEntity entity);
}
