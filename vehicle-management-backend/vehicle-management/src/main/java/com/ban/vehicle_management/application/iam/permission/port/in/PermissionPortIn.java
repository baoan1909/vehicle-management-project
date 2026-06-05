package com.ban.vehicle_management.application.iam.permission.port.in;

import com.ban.vehicle_management.domain.iam.permission.model.Permission;

import java.util.List;

public interface PermissionPortIn {

    List<Permission> getPermissions(String keyword);
}
