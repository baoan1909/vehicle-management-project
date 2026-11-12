package com.ban.vehicle_management.infrastructure.mapper.billing;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessment;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessmentLine;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PlatformFeeAssessmentLineEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlatformFeeAssessmentPersistenceMapper {
    PlatformFeeAssessmentEntity toEntity(PlatformFeeAssessment domain);

    @Mapping(target = "lines", ignore = true)
    PlatformFeeAssessment toDomain(PlatformFeeAssessmentEntity entity);

    PlatformFeeAssessmentLineEntity toLineEntity(PlatformFeeAssessmentLine domain);
    PlatformFeeAssessmentLine toLineDomain(PlatformFeeAssessmentLineEntity entity);
}
