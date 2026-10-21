package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeRuleVersionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlatformFeePersistenceMapper {

    PlatformFeeTypeEntity toTypeEntity(PlatformFeeType domain);

    PlatformFeeType toTypeDomain(PlatformFeeTypeEntity entity);

    PlatformFeeRuleVersionEntity toVersionEntity(PlatformFeeRuleVersion domain);

    PlatformFeeRuleVersion toVersionDomain(PlatformFeeRuleVersionEntity entity);
}
