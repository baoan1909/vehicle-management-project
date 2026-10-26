package com.ban.vehicle_management.application.iam.organization.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.AccountAccessInvalidationPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private OrganizationPortOut organizationPortOut;

    @Mock
    private OrganizationAccessGuard organizationAccessGuard;

    @Mock
    private OrganizationPolicy organizationPolicy;

    @Mock
    private PartnerCatalogSeedPortOut partnerCatalogSeedPortOut;

    @Mock
    private AccountAccessInvalidationPortIn accountAccessInvalidationPortIn;

    @InjectMocks
    private OrganizationUseCaseImpl organizationUseCase;

    @Test
    void assigningOneParkingLotAddsScopeWithoutReplacingExistingScopes() {
        UUID organizationId = UUID.randomUUID();
        UUID managerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        when(organizationPortOut.isActiveAccountWithRole(managerAccountId, OrganizationAccessGuard.PARKING_MANAGER))
                .thenReturn(true);
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId)))
                .thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, managerAccountId))
                .thenReturn(Optional.of(membershipId));

        organizationUseCase.assignParkingManagerToParkingLot(organizationId, managerAccountId, parkingLotId);

        verify(organizationPortOut).addParkingLotScope(membershipId, parkingLotId);
        verify(organizationPortOut, never()).replaceParkingLotScopes(membershipId, Set.of(parkingLotId));
        verify(accountAccessInvalidationPortIn).invalidateAccount(
                managerAccountId,
                "Phạm vi đơn vị hoặc bãi xe được phân công cho tài khoản đã thay đổi.",
                "iam",
                "organizations",
                organizationId
        );
    }

    @Test
    void assigningTheSameParkingLotAgainRemainsAnIdempotentAddOperation() {
        UUID organizationId = UUID.randomUUID();
        UUID managerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        when(organizationPortOut.isActiveAccountWithRole(managerAccountId, OrganizationAccessGuard.PARKING_MANAGER))
                .thenReturn(true);
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId)))
                .thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, managerAccountId))
                .thenReturn(Optional.of(membershipId));

        organizationUseCase.assignParkingManagerToParkingLot(organizationId, managerAccountId, parkingLotId);
        organizationUseCase.assignParkingManagerToParkingLot(organizationId, managerAccountId, parkingLotId);

        verify(organizationPortOut, org.mockito.Mockito.times(2)).addParkingLotScope(membershipId, parkingLotId);
    }

    @Test
    void unassigningRemovesOnlyTheRequestedParkingLotScope() {
        UUID organizationId = UUID.randomUUID();
        UUID managerAccountId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId)))
                .thenReturn(true);
        when(organizationPortOut.findActiveMembershipId(organizationId, managerAccountId))
                .thenReturn(Optional.of(membershipId));

        organizationUseCase.unassignParkingManagerFromParkingLot(organizationId, managerAccountId, parkingLotId);

        verify(organizationPortOut).removeParkingLotScope(membershipId, parkingLotId);
        verify(accountAccessInvalidationPortIn).invalidateAccount(
                managerAccountId,
                "Phạm vi đơn vị hoặc bãi xe được phân công cho tài khoản đã thay đổi.",
                "iam",
                "organizations",
                organizationId
        );
    }

    @Test
    void readsAssignedManagersForTheSelectedParkingLot() {
        UUID organizationId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        Set<UUID> managerIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
        when(organizationPortOut.allParkingLotsBelongToOrganization(organizationId, Set.of(parkingLotId)))
                .thenReturn(true);
        when(organizationPortOut.findActiveParkingManagerAccountIdsByParkingLotId(parkingLotId))
                .thenReturn(managerIds);

        Set<UUID> result = organizationUseCase.getAssignedParkingManagerAccountIds(organizationId, parkingLotId);

        assertEquals(managerIds, result);
    }
}
