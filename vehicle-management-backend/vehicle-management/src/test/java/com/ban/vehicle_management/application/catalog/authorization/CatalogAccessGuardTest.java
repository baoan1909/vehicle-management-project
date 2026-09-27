package com.ban.vehicle_management.application.catalog.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CatalogAccessGuardTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock OrganizationPortOut organizationPortOut;
    @Mock ParkingLotPortOut parkingLotPortOut;
    @InjectMocks CatalogAccessGuard guard;

    @Test
    void partnerCanOnlyReadAndWriteOwnCatalog() {
        UUID accountId = UUID.randomUUID();
        UUID ownOrganization = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(accountId,
                Set.of(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId)).thenReturn(Set.of(ownOrganization));

        assertEquals(ownOrganization, guard.writableOrganizationId());
        assertDoesNotThrow(() -> guard.ensureReadable(ownOrganization));
        assertThrows(AccessDeniedException.class, () -> guard.ensureReadable(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> guard.ensureWritable(UUID.randomUUID()));
    }

    @Test
    void selectedLotResolvesCatalogOwnerForPartnerWithMultipleMemberships() {
        UUID accountId = UUID.randomUUID();
        UUID firstOrganization = UUID.randomUUID();
        UUID secondOrganization = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(secondOrganization);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(accountId,
                Set.of(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId))
                .thenReturn(Set.of(firstOrganization, secondOrganization));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));

        assertEquals(secondOrganization, guard.writableOrganizationId(lotId));
        assertThrows(AccessDeniedException.class, guard::writableOrganizationId);
    }

    @Test
    void selectedLotOutsidePartnerMembershipCannotOwnNewCatalogItem() {
        UUID accountId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(UUID.randomUUID());
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(accountId,
                Set.of(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationPortOut.findActiveOrganizationIdsByAccountId(accountId)).thenReturn(Set.of(UUID.randomUUID()));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));

        assertThrows(AccessDeniedException.class, () -> guard.writableOrganizationId(lotId));
    }

    @Test
    void managerCanReadAssignedPartnerCatalogButCannotWriteIt() {
        UUID accountId = UUID.randomUUID();
        UUID lotId = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(partnerId);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(accountId,
                Set.of(OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)));
        when(organizationPortOut.findScopedParkingLotIdsByAccountId(accountId)).thenReturn(Set.of(lotId));
        when(parkingLotPortOut.findById(lotId)).thenReturn(Optional.of(lot));

        assertDoesNotThrow(() -> guard.ensureReadable(partnerId));
        assertThrows(AccessDeniedException.class, () -> guard.ensureWritable(partnerId));
    }

    @Test
    void platformCanReadAllButCannotWritePartnerCatalog() {
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(UUID.randomUUID(),
                Set.of(OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)));
        assertDoesNotThrow(() -> guard.ensureReadable(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, guard::writableOrganizationId);
    }

    private CurrentAccountAccess account(UUID accountId, Set<String> permissions) {
        return new CurrentAccountAccess(accountId, "subject", "user", "user@example.com",
                UUID.randomUUID(), "TEST_ROLE", AccountStatus.ACTIVE, null, permissions);
    }
}
