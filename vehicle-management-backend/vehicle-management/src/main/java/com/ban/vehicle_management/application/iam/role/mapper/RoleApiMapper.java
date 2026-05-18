package com.ban.vehicle_management.application.iam.role.mapper;

import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.entrypoint.dto.iam.role.request.CreateRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.role.request.UpdateRoleRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.role.response.RoleAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleApiMapper {
    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "isSystem", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Role toDomain(CreateRoleRequest request);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "isSystem", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Role toDomain(UpdateRoleRequest request);

    List<RoleAdminResponse> toAdminResponses(List<Role> roles);

    RoleAdminResponse toAdminResponse(Role role);

    default String map(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }
}
