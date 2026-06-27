package com.ban.vehicle_management.infrastructure.mapper.operations;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftTemplateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftTemplatePersistenceMapper {

    @Mapping(target = "parkingLot", ignore = true)
    ShiftTemplateEntity toEntity(ShiftTemplate shiftTemplate);

    ShiftTemplate toDomain(ShiftTemplateEntity entity);
}