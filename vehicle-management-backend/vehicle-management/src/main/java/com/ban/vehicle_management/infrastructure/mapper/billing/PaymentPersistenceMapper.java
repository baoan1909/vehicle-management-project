package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentPersistenceMapper {

    PaymentEntity toEntity(Payment domain);

    Payment toDomain(PaymentEntity entity);
}


