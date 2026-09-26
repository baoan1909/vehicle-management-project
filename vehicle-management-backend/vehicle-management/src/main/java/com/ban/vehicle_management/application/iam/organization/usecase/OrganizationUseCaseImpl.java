package com.ban.vehicle_management.application.iam.organization.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationUseCaseImpl implements OrganizationPortIn {

    private static final String ORGANIZATION_CREATE_ALL = "ORGANIZATION_CREATE_ALL";
    private static final String ORGANIZATION_READ_ALL = "ORGANIZATION_READ_ALL";
    private static final String ORGANIZATION_MEMBERSHIP_MANAGE_ALL = "ORGANIZATION_MEMBERSHIP_MANAGE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final OrganizationPolicy organizationPolicy;

    public OrganizationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            OrganizationPortOut organizationPortOut,
            OrganizationAccessGuard organizationAccessGuard,
            OrganizationPolicy organizationPolicy
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.organizationPortOut = organizationPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
        this.organizationPolicy = organizationPolicy;
    }

    @Override
    @Transactional
    public Organization createOrganization(Organization organization, UUID partnerAdminAccountId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        ensureSystemAdmin();
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
        return savedOrganization;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> getAccessibleOrganizations() {
        currentAccountPortIn.requirePermission(ORGANIZATION_READ_ALL);
        CurrentAccountAccess currentAccount = currentAccountPortIn.getCurrentAccountOrThrow();
        if (OrganizationAccessGuard.SYSTEM_ADMIN.equals(currentAccount.roleCode())) {
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
        organizationPortOut.replaceParkingLotScopes(membershipId, Set.copyOf(parkingLotIds));
    }

    @Override
    @Transactional
    public void assignParkingManagerToParkingLot(UUID organizationId, UUID parkingManagerAccountId, UUID parkingLotId) {
        Set<UUID> parkingLotIds = new HashSet<>(
                organizationPortOut.findScopedParkingLotIdsByOrganizationIdAndAccountId(
                        organizationId,
                        parkingManagerAccountId
                )
        );
        parkingLotIds.add(parkingLotId);
        assignParkingManager(organizationId, parkingManagerAccountId, parkingLotIds);
    }

    private void ensureSystemAdmin() {
        if (!OrganizationAccessGuard.SYSTEM_ADMIN.equals(currentAccountPortIn.getCurrentAccountOrThrow().roleCode())) {
            throw new AccessDeniedException("Current account is not a system administrator");
        }
    }
}
