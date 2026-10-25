package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.iam.account.policy.EffectiveAccountAccessPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record CurrentAccountAccess(
        UUID accountId,
        String subject,
        String username,
        String email,
        UUID roleId,
        String roleCode,
        boolean roleActive,
        boolean hasActivePartnerOrganizationMembership,
        Set<UUID> activeOrganizationIds,
        Set<UUID> assignedParkingLotIds,
        AccountStatus status,
        EmployeeStatus employeeStatus,
        CustomerStatus customerStatus,
        CustomerApprovalStatus customerApprovalStatus,
        boolean onboardingRequired,
        Set<String> permissionCodes
) {
    private static final EffectiveAccountAccessPolicy EFFECTIVE_ACCESS_POLICY = new EffectiveAccountAccessPolicy();

    public CurrentAccountAccess {
        activeOrganizationIds = activeOrganizationIds == null ? Set.of() : Set.copyOf(activeOrganizationIds);
        assignedParkingLotIds = assignedParkingLotIds == null ? Set.of() : Set.copyOf(assignedParkingLotIds);
        permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
    }

    public CurrentAccountAccess(UUID accountId, String subject, String username, String email, UUID roleId,
            String roleCode, boolean roleActive, boolean hasActivePartnerOrganizationMembership,
            AccountStatus status, EmployeeStatus employeeStatus, CustomerStatus customerStatus,
            CustomerApprovalStatus customerApprovalStatus, Set<String> permissionCodes) {
        this(accountId, subject, username, email, roleId, roleCode, roleActive,
                hasActivePartnerOrganizationMembership, Set.of(), Set.of(), status,
                employeeStatus, customerStatus, customerApprovalStatus, false, permissionCodes);
    }

    public CurrentAccountAccess(UUID accountId, String subject, String username, String email, UUID roleId,
            String roleCode, boolean roleActive, AccountStatus status, EmployeeStatus employeeStatus,
            CustomerStatus customerStatus, CustomerApprovalStatus customerApprovalStatus,
            Set<String> permissionCodes) {
        this(accountId, subject, username, email, roleId, roleCode, roleActive, false,
                Set.of(), Set.of(), status, employeeStatus, customerStatus,
                customerApprovalStatus, false, permissionCodes);
    }

    public CurrentAccountAccess(UUID accountId, String subject, String username, String email, UUID roleId,
            String roleCode, AccountStatus status, EmployeeStatus employeeStatus,
            CustomerStatus customerStatus, CustomerApprovalStatus customerApprovalStatus,
            Set<String> permissionCodes) {
        this(accountId, subject, username, email, roleId, roleCode, true, false,
                Set.of(), Set.of(), status, employeeStatus, customerStatus,
                customerApprovalStatus, false, permissionCodes);
    }

    public CurrentAccountAccess(UUID accountId, String subject, String username, String email, UUID roleId,
            String roleCode, AccountStatus status, EmployeeStatus employeeStatus,
            Set<String> permissionCodes) {
        this(accountId, subject, username, email, roleId, roleCode, true, false,
                Set.of(), Set.of(), status, employeeStatus, null, null, false, permissionCodes);
    }

    public boolean canUseBusinessPermissions() {
        return EFFECTIVE_ACCESS_POLICY.canUseBusinessPermissions(toAccessState());
    }

    public Set<String> getEffectivePermissionCodes() {
        return EFFECTIVE_ACCESS_POLICY.resolveEffectivePermissionCodes(toAccessState(), permissionCodes);
    }

    /** Stable opaque equality token; no PII is included in the canonical input. */
    public String accessRevision() {
        String canonical = String.join("|", value(accountId), value(roleId), value(roleCode),
                Boolean.toString(roleActive), Boolean.toString(hasActivePartnerOrganizationMembership),
                value(status), value(employeeStatus), value(customerStatus), value(customerApprovalStatus),
                Boolean.toString(onboardingRequired), sorted(permissionCodes),
                sorted(activeOrganizationIds), sorted(assignedParkingLotIds));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private EffectiveAccountAccessPolicy.AccessState toAccessState() {
        return new EffectiveAccountAccessPolicy.AccessState(roleCode, roleActive,
                hasActivePartnerOrganizationMembership, activeOrganizationIds, assignedParkingLotIds,
                status, employeeStatus, customerStatus, customerApprovalStatus);
    }

    private String sorted(Set<?> values) {
        return values == null ? "" : values.stream().map(String::valueOf).sorted()
                .collect(Collectors.joining(","));
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
