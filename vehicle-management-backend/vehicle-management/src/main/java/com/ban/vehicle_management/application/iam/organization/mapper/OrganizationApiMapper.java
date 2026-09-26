package com.ban.vehicle_management.application.iam.organization.mapper;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.CreateOrganizationRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationApiMapper {

    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Organization toDomain(CreateOrganizationRequest request);

    OrganizationAdminResponse toAdminResponse(Organization organization);

    List<OrganizationAdminResponse> toAdminResponses(List<Organization> organizations);

}
