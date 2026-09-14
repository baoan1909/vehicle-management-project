package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.voucherredemption.model.VoucherRedemption;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.VoucherRedemptionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherRedemptionPersistenceMapper {
    VoucherRedemptionEntity toEntity(VoucherRedemption domain);
    VoucherRedemption toDomain(VoucherRedemptionEntity entity);
}
