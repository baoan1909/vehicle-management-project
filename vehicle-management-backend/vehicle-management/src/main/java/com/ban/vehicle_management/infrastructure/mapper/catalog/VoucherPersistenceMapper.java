package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VoucherEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherPersistenceMapper {
    VoucherEntity toEntity(Voucher domain);
    Voucher toDomain(VoucherEntity entity);
    List<Voucher> toDomains(List<VoucherEntity> entities);
}
