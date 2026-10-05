package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationPersistenceMapper {
    OrganizationEntity toEntity(Organization organization);

    @Mapping(target = "structuredAddress", ignore = true)
    Organization toDomain(OrganizationEntity entity);
}
