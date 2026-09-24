package com.ban.vehicle_management.infrastructure.mapper.iam;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.OrganizationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationPersistenceMapper {
    OrganizationEntity toEntity(Organization organization);

    Organization toDomain(OrganizationEntity entity);
}
