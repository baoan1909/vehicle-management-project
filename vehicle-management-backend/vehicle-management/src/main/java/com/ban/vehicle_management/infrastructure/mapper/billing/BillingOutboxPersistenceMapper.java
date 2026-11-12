package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.outbox.model.BillingOutboxEvent;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.BillingOutboxEventEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BillingOutboxPersistenceMapper {
    BillingOutboxEvent toDomain(BillingOutboxEventEntity entity);
}
