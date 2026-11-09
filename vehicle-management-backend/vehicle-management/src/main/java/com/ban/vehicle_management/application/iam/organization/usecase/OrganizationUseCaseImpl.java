package com.ban.vehicle_management.application.iam.organization.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.ban.vehicle_management.application.iam.organization.port.in.ParkingLotAssignmentPortIn;
import com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentRole;
import java.time.Instant;

@Service
public class OrganizationUseCaseImpl implements OrganizationPortIn {

    private static final String ORGANIZATION_CREATE_ALL = "ORGANIZATION_CREATE_ALL";
    private static final String ORGANIZATION_READ_ALL = "ORGANIZATION_READ_ALL";
    private static final String ORGANIZATION_MEMBERSHIP_MANAGE_ALL = "ORGANIZATION_MEMBERSHIP_MANAGE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final OrganizationPolicy organizationPolicy;
    private final PartnerCatalogSeedPortOut partnerCatalogSeedPortOut;
    private final AccountAccessInvalidationPortIn accountAccessInvalidationPortIn;
    private ParkingLotAssignmentPortIn parkingLotAssignmentPortIn;

    @Autowired(required = false)
    void configureParkingLotAssignments(ParkingLotAssignmentPortIn parkingLotAssignmentPortIn) {
        this.parkingLotAssignmentPortIn = parkingLotAssignmentPortIn;
    }

    public OrganizationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationPortOut organizationPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            OrganizationPolicy organizationPolicy,
            PartnerCatalogSeedPortOut partnerCatalogSeedPortOut,
            AccountAccessInvalidationPortIn accountAccessInvalidationPortIn
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.organizationPolicy = organizationPolicy;
        this.partnerCatalogSeedPortOut = partnerCatalogSeedPortOut;
        this.accountAccessInvalidationPortIn = accountAccessInvalidationPortIn;
    }

    @Override
    @Transactional
    public Organization createOrganization(Organization organization, UUID partnerAdminAccountId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        organizationPolicy.initialize(organization);
        if (organizationPortOut.existsByCode(organization.getCode())) {
            throw new ConflictException("Organization code already exists");
        }
        if (!organizationPortOut.isActiveAccountWithRole(partnerAdminAccountId, OrganizationAccessGuard.PARTNER_ADMIN)) {
            throw new NotFoundException("Active PARTNER_ADMIN account not found");
        }
        organization.setOrganizationId(UUID.randomUUID());
        Organization savedOrganization = organizationPortOut.save(organization);
        organizationPortOut.createActiveMembership(savedOrganization.getOrganizationId(), partnerAdminAccountId);
        partnerCatalogSeedPortOut.copyInternalCatalog(savedOrganization.getOrganizationId());
        invalidateOrganizationAccess(partnerAdminAccountId, savedOrganization.getOrganizationId());
        return savedOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getAccessibleOrganizations() {
        currentAccountPortIn.requirePermission(ORGANIZATION_READ_ALL);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (currentAccountPortIn.hasPermission(ORGANIZATION_CREATE_ALL)) {
            return organizationPortOut.findAll();
        }
        return organizationPortOut.findAllByIds(
                organizationPortOut.findActiveOrganizationIdsByAccountId(currentAccount.accountId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Organization getOrganizationById(UUID organizationId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_READ_ALL);
        organizationAccessGuard.ensureCanAccessOrganization(organizationId);
        return organizationPortOut.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
    }

    @Override
    @Transactional
    public void assignParkingManager(UUID organizationId, UUID parkingManagerAccountId, Set<UUID> parkingLotIds) {
        currentAccountPortIn.requirePermission(ORGANIZATION_MEMBERSHIP_MANAGE_ALL);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        if (!organizationPortOut.isActiveAccountWithRole(parkingManagerAccountId, OrganizationAccessGuard.PARKING_MANAGER)) {
            throw new NotFoundException("Active PARKING_MANAGER account not found");
        }
        if (!organizationPortOut.allParkingLotsBelongToOrganization(organizationId, parkingLotIds)) {
            throw new ConflictException("Every assigned parking lot must belong to the organization");
        }
        UUID membershipId = organizationPortOut.findActiveMembershipId(organizationId, parkingManagerAccountId)
                .orElseGet(() -> {
                    organizationPortOut.createActiveMembership(organizationId, parkingManagerAccountId);
                    return organizationPortOut.findActiveMembershipId(organizationId, parkingManagerAccountId)
                            .orElseThrow(() -> new IllegalStateException("Organization membership was not created"));
                });
        if (parkingLotAssignmentPortIn != null) {
            for (UUID parkingLotId : parkingLotIds) {
                if (!parkingLotAssignmentPortIn.getManagementHistory(organizationId, parkingLotId).stream()
                        .anyMatch(item -> parkingManagerAccountId.equals(item.getManagerAccountId())
                                && com.ban.vehicle_management.shared.enumeration.iam.ManagementAssignmentStatus.ACTIVE.equals(item.getStatus()))) {
                    parkingLotAssignmentPortIn.assignManager(
                            organizationId, parkingLotId, parkingManagerAccountId,
                            ManagementAssignmentRole.SUPPORT, Instant.now(), false, "Migrated from legacy scope endpoint");
                }
            }
        } else {
            organizationPortOut.replaceParkingLotScopes(membershipId, Set.copyOf(parkingLotIds));
        }
        invalidateOrganizationAccess(parkingManagerAccountId, organizationId);
    }

    @Override
    @Transactional
    public void assignParkingManagerToParkingLot(UUID organizationId, UUID parkingManagerAccountId, UUID parkingLotId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_MEMBERSHIP_MANAGE_ALL);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        if (!organizationPortOut.isActiveAccountWithRole(
                parkingManagerAccountId,
                OrganizationAccessGuard.PARKING_MANAGER
        )) {
            throw new NotFoundException("Active PARKING_MANAGER account not found");
        }
        if (!organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId))) {
            throw new ConflictException("Assigned parking lot must belong to the organization");
        }
        UUID membershipId = findOrCreateActiveMembership(organizationId, parkingManagerAccountId);
        if (parkingLotAssignmentPortIn != null) {
            parkingLotAssignmentPortIn.assignManager(
                    organizationId, parkingLotId, parkingManagerAccountId,
                    ManagementAssignmentRole.PRIMARY, Instant.now(), false, "Assigned through compatibility endpoint");
        } else {
            organizationPortOut.addParkingLotScope(membershipId, parkingLotId);
        }
        invalidateOrganizationAccess(parkingManagerAccountId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getAssignedParkingManagerAccountIds(UUID organizationId, UUID parkingLotId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_MEMBERSHIP_MANAGE_ALL);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        if (!organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId))) {
            throw new NotFoundException("Parking lot not found in organization");
        }
        return organizationPortOut.findActiveParkingManagerAccountIdsByParkingLotId(parkingLotId);
    }

    @Override
    public com.ban.vehicle_management.application.iam.organization.model.result.ParkingManagerScopeResult
            getAssignedParkingManagerScope(UUID organizationId, UUID parkingLotId) {
        return new com.ban.vehicle_management.application.iam.organization.model.result.ParkingManagerScopeResult(
                getAssignedParkingManagerAccountIds(organizationId, parkingLotId));
    }

    @Override
    @Transactional
    public void unassignParkingManagerFromParkingLot(
            UUID organizationId,
            UUID parkingManagerAccountId,
            UUID parkingLotId
    ) {
        currentAccountPortIn.requirePermission(ORGANIZATION_MEMBERSHIP_MANAGE_ALL);
        organizationAccessGuard.ensureCanManageOrganization(organizationId);
        if (!organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId))) {
            throw new NotFoundException("Parking lot not found in organization");
        }
        if (parkingLotAssignmentPortIn != null) {
            parkingLotAssignmentPortIn.endManagerAssignment(
                    organizationId, parkingLotId, parkingManagerAccountId, "Revoked through compatibility endpoint");
        } else {
            organizationPortOut.findActiveMembershipId(organizationId, parkingManagerAccountId)
                    .ifPresent(membershipId -> organizationPortOut.removeParkingLotScope(membershipId, parkingLotId));
        }
        invalidateOrganizationAccess(parkingManagerAccountId, organizationId);
    }

    private UUID findOrCreateActiveMembership(UUID organizationId, UUID parkingManagerAccountId) {
        return organizationPortOut.findActiveMembershipId(organizationId, parkingManagerAccountId)
                .orElseGet(() -> {
                    organizationPortOut.createActiveMembership(organizationId, parkingManagerAccountId);
                    return organizationPortOut.findActiveMembershipId(organizationId, parkingManagerAccountId)
                            .orElseThrow(() -> new IllegalStateException("Organization membership was not created"));
                });
    }

    private void invalidateOrganizationAccess(UUID accountId, UUID organizationId) {
        accountAccessInvalidationPortIn.invalidateAccount(
                accountId,
                "Phạm vi đơn vị hoặc bãi xe được phân công cho tài khoản đã thay đổi.",
                "iam",
                "organizations",
                organizationId
        );
    }

}
