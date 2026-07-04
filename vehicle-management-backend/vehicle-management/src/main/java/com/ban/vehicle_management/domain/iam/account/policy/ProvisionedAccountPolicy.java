package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProvisionedAccountPolicy {

    public Set<AdminProvisionableAccountRoleCode> managedTargetRoles(AdminProvisionableAccountRoleCode currentRole) {
        AdminProvisionableAccountRoleCode role = requireProvisionableRole(currentRole);
        return switch (role) {
            case SYSTEM_ADMIN -> Set.of(
                    AdminProvisionableAccountRoleCode.SYSTEM_ADMIN,
                    AdminProvisionableAccountRoleCode.PARKING_MANAGER
            );
            case PARKING_MANAGER -> Set.of(
                    AdminProvisionableAccountRoleCode.EMPLOYEE,
                    AdminProvisionableAccountRoleCode.CUSTOMER
            );
            case CUSTOMER, EMPLOYEE -> Set.of();
        };
    }

    public boolean canManageTargetRole(
            AdminProvisionableAccountRoleCode currentRole,
            AdminProvisionableAccountRoleCode targetRole
    ) {
        return managedTargetRoles(currentRole).contains(requireProvisionableRole(targetRole));
    }

    public AccountStatus initialAccountStatus(AdminProvisionableAccountRoleCode roleCode) {
        return AdminProvisionableAccountRoleCode.SYSTEM_ADMIN.equals(requireProvisionableRole(roleCode))
                ? AccountStatus.PENDING
                : AccountStatus.ACTIVE;
    }

    public UserProfileStatus toUserProfileStatus(AccountStatus accountStatus) {
        return switch (requireAccountStatus(accountStatus)) {
            case ACTIVE -> UserProfileStatus.ACTIVE;
            case LOCKED -> UserProfileStatus.SUSPENDED;
            case DISABLED -> UserProfileStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    public EmployeeStatus toEmployeeStatus(
            AdminProvisionableAccountRoleCode roleCode,
            AccountStatus accountStatus
    ) {
        if (!requireProvisionableRole(roleCode).requiresEmployeeRecord()) {
            return null;
        }
        return switch (requireAccountStatus(accountStatus)) {
            case ACTIVE -> EmployeeStatus.ACTIVE;
            case LOCKED -> EmployeeStatus.SUSPENDED;
            case DISABLED -> EmployeeStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    public CustomerStatus toCustomerStatus(AccountStatus accountStatus) {
        return switch (requireAccountStatus(accountStatus)) {
            case ACTIVE -> CustomerStatus.ACTIVE;
            case LOCKED, DISABLED -> CustomerStatus.INACTIVE;
            case PENDING -> throw new BadRequestException("Provisioned accounts do not support PENDING status");
        };
    }

    public void validateStatusTransition(AccountStatus currentStatus, AccountStatus targetStatus) {
        if (AccountStatus.DISABLED.equals(requireAccountStatus(currentStatus))
                && AccountStatus.LOCKED.equals(requireAccountStatus(targetStatus))) {
            throw new BadRequestException("Provisioned accounts cannot transition from DISABLED to LOCKED");
        }
    }

    public void validateRoleTransition(
            AdminProvisionableAccountRoleCode currentRole,
            AdminProvisionableAccountRoleCode targetRole
    ) {
        if (requireProvisionableRole(currentRole).isInternalRole()
                != requireProvisionableRole(targetRole).isInternalRole()) {
            throw new BadRequestException(
                    "Changing role between CUSTOMER and internal account types is not supported"
            );
        }
    }

    public AdminProvisionableAccountRoleCode requireProvisionableRole(AdminProvisionableAccountRoleCode roleCode) {
        if (roleCode == null) {
            throw new BadRequestException("roleCode must not be null");
        }
        return roleCode;
    }

    public AdminProvisionableAccountRoleCode requireProvisionableRole(String roleCode) {
        AdminProvisionableAccountRoleCode resolvedRole = resolveProvisionableRole(roleCode);
        if (resolvedRole == null) {
            throw new BadRequestException("Current account role is not supported");
        }
        return resolvedRole;
    }

    public AdminProvisionableAccountRoleCode resolveProvisionableRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        try {
            return AdminProvisionableAccountRoleCode.valueOf(roleCode);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AccountStatus requireAccountStatus(AccountStatus status) {
        if (status == null) {
            throw new BadRequestException("status must not be null");
        }
        return status;
    }
}
