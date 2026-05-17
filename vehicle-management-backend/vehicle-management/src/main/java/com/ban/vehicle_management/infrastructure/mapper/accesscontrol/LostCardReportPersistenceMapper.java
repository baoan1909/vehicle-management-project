package com.ban.vehicle_management.infrastructure.mapper.accesscontrol;

import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.LostCardReportEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LostCardReportPersistenceMapper {

    LostCardReportEntity toEntity(LostCardReport domain);

    LostCardReport toDomain(LostCardReportEntity entity);
}


