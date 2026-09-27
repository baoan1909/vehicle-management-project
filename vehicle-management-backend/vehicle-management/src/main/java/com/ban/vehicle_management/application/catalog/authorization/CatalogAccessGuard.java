package com.ban.vehicle_management.application.catalog.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Resolves catalog ownership from scope permissions and current memberships, never from role names. */
@Component
public class CatalogAccessGuard {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final ParkingLotPortOut parkingLotPortOut;

    public CatalogAccessGuard(CurrentAccountPortIn currentAccountPortIn,
                              OrganizationPortOut organizationPortOut,
                              ParkingLotPortOut parkingLotPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    /** Null means platform-wide read; an empty set means no visible catalog. */
    public Set<UUID> visibleOrganizationIds() {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        Set<String> permissions = account.getEffectivePermissionCodes();
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)) {
            return null;
        }
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)) {
            return organizationPortOut.findActiveOrganizationIdsByAccountId(account.accountId());
        }
        if (permissions.contains(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)) {
            return organizationPortOut.findScopedParkingLotIdsByAccountId(account.accountId()).stream()
                    .map(parkingLotPortOut::findById)
                    .flatMap(java.util.Optional::stream)
                    .map(ParkingLot::getOrganizationId)
                    .collect(Collectors.toUnmodifiableSet());
        }
        throw new AccessDeniedException("Current account has no catalog scope");
    }

    public void ensureReadable(UUID organizationId) {
        Set<UUID> visible = visibleOrganizationIds();
        if (organizationId == null || (visible != null && !visible.contains(organizationId))) {
            throw new AccessDeniedException("Current account cannot access this catalog item");
        }
    }

    /** Shared Partner catalog writes need exactly one owning organization. */
    public UUID writableOrganizationId() {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        if (!account.getEffectivePermissionCodes().contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)) {
            throw new AccessDeniedException("Current account cannot change a Partner catalog");
        }
        Set<UUID> organizationIds = organizationPortOut.findActiveOrganizationIdsByAccountId(account.accountId());
        if (organizationIds.size() != 1) {
            throw new AccessDeniedException("Catalog write requires exactly one active Partner membership");
        }
        return organizationIds.iterator().next();
    }

    /** Resolve a new catalog item's owner from an explicitly selected, authorized lot. */
    public UUID writableOrganizationId(UUID parkingLotId) {
        if (parkingLotId == null) return writableOrganizationId();
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new AccessDeniedException("Selected parking lot is not accessible"));
        ensureWritable(lot.getOrganizationId());
        return lot.getOrganizationId();
    }

    public void ensureWritable(UUID organizationId) {
        CurrentAccountAccess account = currentAccountPortIn.getCurrentAccountOrThrow();
        if (organizationId == null
                || !account.getEffectivePermissionCodes().contains(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                || !organizationPortOut.findActiveOrganizationIdsByAccountId(account.accountId()).contains(organizationId)) {
            throw new AccessDeniedException("Current account cannot change this Partner catalog item");
        }
    }
}
