package com.ban.vehicle_management.application.iam.organization.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OrganizationAccessGuard {

    // Target-account role codes used by organization assignment workflows; never actor authorization.
    public static final String PARTNER_ADMIN = "PARTNER_ADMIN";
    public static final String PARKING_MANAGER = "PARKING_MANAGER";
    public static final String PARKING_TOPOLOGY_CONFIGURE_ALL = "PARKING_TOPOLOGY_CONFIGURE_ALL";
    public static final String PARKING_SCOPE_PLATFORM = "PARKING_SCOPE_PLATFORM";
    public static final String PARKING_SCOPE_PARTNER = "PARKING_SCOPE_PARTNER";
    public static final String PARKING_SCOPE_ASSIGNED = "PARKING_SCOPE_ASSIGNED";
    public static final String PARKING_LOT_CREATE_ALL = "PARKING_LOT_CREATE_ALL";
    public static final String ORGANIZATION_MEMBERSHIP_MANAGE_ALL = "ORGANIZATION_MEMBERSHIP_MANAGE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;

    public OrganizationAccessGuard(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationPortOut organizationPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
    }

    public UUID resolveOrganizationIdForParkingLotCreation(UUID requestedOrganizationId) {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        requirePermission(currentAccount, PARKING_LOT_CREATE_ALL);
        requirePermission(currentAccount, PARKING_SCOPE_PARTNER);

        Set<UUID> organizationIds = currentAccount.activeOrganizationIds();
        if (requestedOrganizationId != null && organizationIds.contains(requestedOrganizationId)) {
            requireActiveOrganization(requestedOrganizationId);
            return requestedOrganizationId;
        }
        if (requestedOrganizationId == null && organizationIds.size() == 1) {
            UUID organizationId = organizationIds.iterator().next();
            requireActiveOrganization(organizationId);
            return organizationId;
        }
        throw new AccessDeniedException("Current account cannot create parking lots for the requested organization");
    }

    public ParkingLotAccessScope resolveParkingLotAccessScope() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = currentAccount.getEffectivePermissionCodes();
        if (permissions.contains(PARKING_SCOPE_PLATFORM)) {
            return ParkingLotAccessScope.unrestrictedScope();
        }
        if (permissions.contains(PARKING_SCOPE_PARTNER)) {
            return new ParkingLotAccessScope(
                    false,
                    currentAccount.activeOrganizationIds(),
                    Set.of()
            );
        }
        if (permissions.contains(PARKING_SCOPE_ASSIGNED)) {
            return new ParkingLotAccessScope(
                    false,
                    Set.of(),
                    currentAccount.assignedParkingLotIds()
            );
        }
        throw new AccessDeniedException("Current account is not assigned to a parking organization");
    }

    public void ensureCanAccessParkingLot(ParkingLot parkingLot) {
        if (parkingLot == null || parkingLot.getOrganizationId() == null) {
            throw new BadRequestException("parkingLot organizationId must not be null");
        }
        ParkingLotAccessScope scope = resolveParkingLotAccessScope();
        if (scope.unrestricted()
                || scope.organizationIds().contains(parkingLot.getOrganizationId())
                || scope.parkingLotIds().contains(parkingLot.getParkingLotId())) {
            return;
        }
        throw new AccessDeniedException("Current account cannot access this parking lot");
    }

    public void ensureCanManageParkingLot(ParkingLot parkingLot) {
        ensureCanAccessParkingLot(parkingLot);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        requirePermission(currentAccount, PARKING_SCOPE_PARTNER);
    }

    /** Null means platform-wide access; an empty set means no accessible lots. */
    public Set<UUID> resolveAccessibleParkingLotIds(ParkingLotPortOut parkingLotPortOut) {
        ParkingLotAccessScope scope = resolveParkingLotAccessScope();
        if (scope.unrestricted()) {
            return null;
        }
        if (!scope.parkingLotIds().isEmpty()) {
            return scope.parkingLotIds();
        }
        if (scope.organizationIds().isEmpty()) {
            return Set.of();
        }
        return parkingLotPortOut.findAll(null, null, scope.organizationIds(), null).stream()
                .map(ParkingLot::getParkingLotId)
                .collect(Collectors.toSet());
    }

    /** Operational writes are reserved for the owning Partner or an assigned Manager. */
    public void ensureCanOperateParkingLot(ParkingLot parkingLot) {
        ensureCanAccessParkingLot(parkingLot);
        Set<String> permissions = currentAccountPortIn.getCurrentAccountOrThrow().getEffectivePermissionCodes();
        if (!permissions.contains(PARKING_SCOPE_PARTNER)
                && !permissions.contains(PARKING_SCOPE_ASSIGNED)) {
            throw new AccessDeniedException("Current account cannot operate this parking lot");
        }
    }

    /**
     * Topology is configured by the Partner operating a parking lot. System Admin
     * may inspect every topology, but must never become an operational operator.
     */
    public void ensureCanConfigureParkingLot(ParkingLot parkingLot) {
        ensureCanAccessParkingLot(parkingLot);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = currentAccount.getEffectivePermissionCodes();
        if (permissions.contains(PARKING_TOPOLOGY_CONFIGURE_ALL)
                && (permissions.contains(PARKING_SCOPE_PARTNER)
                || permissions.contains(PARKING_SCOPE_ASSIGNED))) {
            return;
        }
        throw new AccessDeniedException("Current account cannot configure this parking lot");
    }

    public void ensureCanManageOrganization(UUID organizationId) {
        requireActiveOrganization(organizationId);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (currentAccount.getEffectivePermissionCodes().contains(ORGANIZATION_MEMBERSHIP_MANAGE_ALL)
                && currentAccount.getEffectivePermissionCodes().contains(PARKING_SCOPE_PARTNER)
                && currentAccount.activeOrganizationIds().contains(organizationId)) {
            return;
        }
        throw new AccessDeniedException("Current account cannot manage this organization");
    }

    /**
     * A manager account may finish profile onboarding without a parking-lot scope,
     * but it must not access operational resources until a Partner Admin assigns
     * at least one parking lot to that account.
     */
    public void ensureCurrentManagerHasParkingLotAssignment() {
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!currentAccount.permissionCodes().contains(PARKING_SCOPE_ASSIGNED)) {
            return;
        }
        if (currentAccount.assignedParkingLotIds().isEmpty()) {
            throw new AccessDeniedException(
                    "Parking manager has not been assigned to a parking lot. Please contact the Partner Admin."
            );
        }
    }

    public boolean isCurrentParkingManager() {
        return currentAccountPortIn.getCurrentAccountOrThrow().permissionCodes()
                .contains(PARKING_SCOPE_ASSIGNED);
    }

    public void ensureCanAccessOrganization(UUID organizationId) {
        requireActiveOrganization(organizationId);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = currentAccount.getEffectivePermissionCodes();
        if (permissions.contains(PARKING_SCOPE_PLATFORM)
                || (permissions.contains(PARKING_SCOPE_PARTNER)
                && currentAccount.activeOrganizationIds().contains(organizationId))) {
            return;
        }
        throw new AccessDeniedException("Current account cannot access this organization");
    }

    private void requireActiveOrganization(UUID organizationId) {
        boolean active = organizationPortOut.findById(organizationId)
                .map(organization -> com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus.ACTIVE.equals(organization.getStatus()))
                .orElse(false);
        if (!active) {
            throw new AccessDeniedException("Organization is not active or does not exist");
        }
    }

    private void requirePermission(CurrentAccountAccess account, String permissionCode) {
        if (!account.getEffectivePermissionCodes().contains(permissionCode)) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
