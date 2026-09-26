package com.ban.vehicle_management.application.billing.invoice.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class InvoiceAccessGuardTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountProfilePortOut accountProfilePortOut;
    @Mock OrganizationAccessGuard organizationAccessGuard;
    @Mock ParkingLotPortOut parkingLotPortOut;
    @InjectMocks InvoiceAccessGuard guard;

    @Test
    void partnerCanReadAndOperateOnlyOwnedParkingLot() {
        UUID ownLot = UUID.randomUUID();
        UUID otherLot = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission("INVOICE_READ_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(ownLot));

        assertDoesNotThrow(() -> guard.ensureCanRead(invoice(ownLot)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanRead(invoice(otherLot)));
        assertDoesNotThrow(() -> guard.ensureCanCreateAtParkingLot(ownLot));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCreateAtParkingLot(otherLot));
        assertDoesNotThrow(() -> guard.ensureCanCancel(invoice(ownLot)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCancel(invoice(otherLot)));
        assertEquals(Set.of(ownLot), guard.visibleParkingLotIdsForManagement(null, null));
    }

    @Test
    void managerIsLimitedToAssignedParkingLot() {
        UUID assignedLot = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(assignedLot));

        assertDoesNotThrow(() -> guard.ensureCanCreateAtParkingLot(assignedLot));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCreateAtParkingLot(UUID.randomUUID()));
    }

    @Test
    void platformCanReadAllInvoicesButCannotOperateThem() {
        UUID lotId = UUID.randomUUID();
        when(currentAccountPortIn.hasPermission("INVOICE_READ_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PLATFORM)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(null);

        assertDoesNotThrow(() -> guard.ensureCanRead(invoice(lotId)));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCreateAtParkingLot(lotId));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCancel(invoice(lotId)));
    }

    @Test
    void managementFilterCannotRequestAnotherParkingLot() {
        UUID ownLot = UUID.randomUUID();
        UUID otherLot = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(ownLot));
        when(parkingLotPortOut.findById(otherLot)).thenReturn(java.util.Optional.of(lot(otherLot, UUID.randomUUID())));

        assertThrows(AccessDeniedException.class,
                () -> guard.visibleParkingLotIdsForManagement(null, otherLot));
    }

    @Test
    void managementPartnerFilterIntersectsAccessibleLots() {
        UUID partnerId = UUID.randomUUID();
        UUID ownLot = UUID.randomUUID();
        UUID otherLot = UUID.randomUUID();
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(ownLot));
        when(parkingLotPortOut.findAll(null, null, Set.of(partnerId), null))
                .thenReturn(List.of(lot(ownLot, partnerId), lot(otherLot, partnerId)));

        assertEquals(Set.of(ownLot), guard.visibleParkingLotIdsForManagement(partnerId, null));
    }

    private CurrentAccountAccess account(Set<String> permissions) {
        return new CurrentAccountAccess(UUID.randomUUID(), "subject", "user", "user@example.com",
                UUID.randomUUID(), "TEST_ROLE", AccountStatus.ACTIVE, null, permissions);
    }

    private Invoice invoice(UUID lotId) {
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(lotId);
        return invoice;
    }

    private ParkingLot lot(UUID lotId, UUID partnerId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(partnerId);
        return lot;
    }
}
