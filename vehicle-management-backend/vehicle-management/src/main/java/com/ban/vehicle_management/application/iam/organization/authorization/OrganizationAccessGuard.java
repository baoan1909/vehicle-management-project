package com.ban.vehicle_management.application.iam.organization.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.model.result.ParkingLotAccessScope;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OrganizationAccessGuard {

    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String PARTNER_ADMIN = "PARTNER_ADMIN";
    public static final String PARKING_MANAGER = "PARKING_MANAGER";

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
        if (isSystemAdmin(currentAccount)) {
            if (requestedOrganizationId == null) {
                throw new BadRequestException("organizationId must not be null");
            }
            requireActiveOrganization(requestedOrganizationId);
            return requestedOrganizationId;
        }

        if (!PARTNER_ADMIN.equals(currentAccount.roleCode())) {
            throw new AccessDeniedException("Current account cannot create parking lots");
        }

        Set<UUID> organizationIds = organizationPortOut.findActiveOrganizationIdsByAccountId(currentAccount.accountId());
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
        if (isSystemAdmin(currentAccount)) {
            return ParkingLotAccessScope.unrestrictedScope();
        }
        if (PARTNER_ADMIN.equals(currentAccount.roleCode())) {
            return new ParkingLotAccessScope(
                    false,
                    organizationPortOut.findActiveOrganizationIdsByAccountId(currentAccount.accountId()),
                    Set.of()
            );
        }
        if (PARKING_MANAGER.equals(currentAccount.roleCode())) {
            return new ParkingLotAccessScope(
                    false,
                    Set.of(),
                    organizationPortOut.findScopedParkingLotIdsByAccountId(currentAccount.accountId())
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
        if (!isSystemAdmin(currentAccount) && !PARTNER_ADMIN.equals(currentAccount.roleCode())) {
            throw new AccessDeniedException("Current account cannot manage this parking lot");
        }
    }

    public void ensureCanManageOrganization(UUID organizationId) {
        requireActiveOrganization(organizationId);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (isSystemAdmin(currentAccount)) {
            return;
        }
        if (PARTNER_ADMIN.equals(currentAccount.roleCode())
                && organizationPortOut.findActiveOrganizationIdsByAccountId(currentAccount.accountId()).contains(organizationId)) {
            return;
        }
        throw new AccessDeniedException("Current account cannot manage this organization");
    }

    public void ensureCanAccessOrganization(UUID organizationId) {
        requireActiveOrganization(organizationId);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (isSystemAdmin(currentAccount)
                || organizationPortOut.findActiveOrganizationIdsByAccountId(currentAccount.accountId()).contains(organizationId)) {
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

    private boolean isSystemAdmin(CurrentAccountAccess currentAccount) {
        return SYSTEM_ADMIN.equals(currentAccount.roleCode());
    }
}
