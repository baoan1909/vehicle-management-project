package com.ban.vehicle_management.application.iam.permission.mapper;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.entrypoint.dto.iam.permission.response.PermissionAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PermissionApiMapper {

    List<PermissionAdminResponse> toAdminResponses(List<Permission> permissions);

    PermissionAdminResponse toAdminResponse(Permission permission);

    default String map(Instant value) {
        return DateTimeUtils.formatInstant(value, DateTimeUtils.VIETNAM_ZONE);
    }
}
