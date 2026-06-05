package com.ban.vehicle_management.application.iam.permission.usecase;

import com.ban.vehicle_management.application.iam.permission.port.in.PermissionPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.domain.iam.permission.model.Permission;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionUseCaseImpl implements PermissionPortIn {

    private final PermissionPortOut permissionPortOut;

    public PermissionUseCaseImpl(PermissionPortOut permissionPortOut) {
        this.permissionPortOut = permissionPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> getPermissions(String keyword) {
        return permissionPortOut.findAll(
                TextValidationUtils.normalizeNullableText(keyword, "keyword", 255)
        );
    }
}
