package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Pure, deterministic registry for permission-specific lifecycle gates. */
public final class PermissionAccessPrerequisitePolicy {

    private static final Pattern PERMISSION_CODE_PATTERN = Pattern.compile("[A-Z0-9_-]+");
    private static final Set<String> CUSTOMER_OWN_PREFIXES = Set.of(
            "CUSTOMER_VEHICLE_", "SUBSCRIPTION_", "WALLET_"
    );
    private static final Set<String> CUSTOMER_OWN_PERMISSIONS = Set.of(
            "PARKING_SESSION_READ_OWN", "INVOICE_READ_OWN", "PAYMENT_CREATE_OWN"
    );
    private static final Set<String> EMPLOYEE_PERMISSIONS = Set.of(
            "EMPLOYEE_READ_OWN", "SHIFT_OPEN_OWN", "SHIFT_ASSIGNMENT_READ_OWN", "PARKING_SCOPE_ASSIGNED"
    );
    private static final Set<String> EMPLOYEE_OWN_PREFIXES = Set.of("EMPLOYEE_", "SHIFT_");
    private static final Set<String> PARTNER_MEMBERSHIP_PERMISSIONS = Set.of(
            "PARKING_SCOPE_PARTNER", "WALLET_SCOPE_PARTNER",
            "ORGANIZATION_PROFILE_READ_OWN", "ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN",
            "ONBOARDING_APPROVAL_REVIEW_PARKING_MANAGER_OWN"
    );
    private static final String REVIEW_EMPLOYEE_PERMISSION =
            "ONBOARDING_APPROVAL_REVIEW_EMPLOYEE_ALL";

    public Set<String> normalizeAssignedPermissions(Collection<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Set.of();
        }
        return permissionCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .filter(code -> PERMISSION_CODE_PATTERN.matcher(code).matches())
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isSatisfied(
            String permissionCode,
            EffectiveAccountAccessPolicy.AccessState state
    ) {
        if (permissionCode == null || state == null) {
            return false;
        }
        return switch (classificationFor(permissionCode)) {
            case CUSTOMER_LIFECYCLE -> CustomerStatus.ACTIVE.equals(state.customerStatus())
                    && CustomerApprovalStatus.APPROVED.equals(state.customerApprovalStatus());
            case EMPLOYEE_LIFECYCLE -> EmployeeStatus.ACTIVE.equals(state.employeeStatus())
                    && (!"PARKING_SCOPE_ASSIGNED".equals(permissionCode)
                    || !state.assignedParkingLotIds().isEmpty());
            case PARTNER_ORGANIZATION -> state.hasActivePartnerOrganizationMembership()
                    && !state.activeOrganizationIds().isEmpty();
            case EMPLOYEE_OR_PARTNER_REVIEW -> (!state.activeOrganizationIds().isEmpty()
                    && state.hasActivePartnerOrganizationMembership())
                    || EmployeeStatus.ACTIVE.equals(state.employeeStatus());
            case COMMON_ACCOUNT_ROLE, RESOURCE_OWNERSHIP -> true;
        };
    }

    public PermissionPrerequisite classificationFor(String permissionCode) {
        if (permissionCode == null) {
            return PermissionPrerequisite.COMMON_ACCOUNT_ROLE;
        }
        if (requiresCustomerLifecycle(permissionCode)) {
            return PermissionPrerequisite.CUSTOMER_LIFECYCLE;
        }
        if (REVIEW_EMPLOYEE_PERMISSION.equals(permissionCode)) {
            return PermissionPrerequisite.EMPLOYEE_OR_PARTNER_REVIEW;
        }
        if (EMPLOYEE_PERMISSIONS.contains(permissionCode)
                || (permissionCode.endsWith("_OWN")
                && EMPLOYEE_OWN_PREFIXES.stream().anyMatch(permissionCode::startsWith))) {
            return PermissionPrerequisite.EMPLOYEE_LIFECYCLE;
        }
        if (requiresPartnerMembership(permissionCode)) {
            return PermissionPrerequisite.PARTNER_ORGANIZATION;
        }
        if (permissionCode.endsWith("_OWN")) {
            return PermissionPrerequisite.RESOURCE_OWNERSHIP;
        }
        return PermissionPrerequisite.COMMON_ACCOUNT_ROLE;
    }

    private boolean requiresCustomerLifecycle(String permissionCode) {
        if (!permissionCode.endsWith("_OWN") && !"WALLET_SCOPE_OWN".equals(permissionCode)) {
            return false;
        }
        return CUSTOMER_OWN_PERMISSIONS.contains(permissionCode)
                || CUSTOMER_OWN_PREFIXES.stream().anyMatch(permissionCode::startsWith);
    }

    private boolean requiresPartnerMembership(String permissionCode) {
        return permissionCode.endsWith("_PARTNER")
                || PARTNER_MEMBERSHIP_PERMISSIONS.contains(permissionCode);
    }

    public enum PermissionPrerequisite {
        COMMON_ACCOUNT_ROLE,
        CUSTOMER_LIFECYCLE,
        EMPLOYEE_LIFECYCLE,
        PARTNER_ORGANIZATION,
        EMPLOYEE_OR_PARTNER_REVIEW,
        RESOURCE_OWNERSHIP
    }
}
