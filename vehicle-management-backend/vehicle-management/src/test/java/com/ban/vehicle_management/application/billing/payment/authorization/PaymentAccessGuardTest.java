package com.ban.vehicle_management.application.billing.payment.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.payment.port.out.EmployeePaymentScopePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountProfilePortOut;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PaymentAccessGuardTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock AccountProfilePortOut accountProfilePortOut;
    @Mock OrganizationAccessGuard organizationAccessGuard;
    @Mock ParkingLotPortOut parkingLotPortOut;
    @Mock EmployeePaymentScopePortOut employeePaymentScopePortOut;
    @InjectMocks PaymentAccessGuard guard;

    @Test
    void paymentListUsesInvoiceParkingLotScope() {
        UUID lotId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of(lotId));

        assertEquals(Set.of(lotId), guard.visibleParkingLotIdsForList());
        verify(currentAccountPortIn).requirePermission("PAYMENT_READ_ALL");
    }

    @Test
    void adminVnpayPaymentCannotBypassInvoiceScope() {
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(UUID.randomUUID());
        when(currentAccountPortIn.hasPermission("PAYMENT_CREATE_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_ASSIGNED)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut)).thenReturn(Set.of());

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanCreateVnpayPayment(invoice));
    }

    @Test
    void scopedAdminCanCreateVnpayPayment() {
        Invoice invoice = new Invoice();
        invoice.setParkingLotId(UUID.randomUUID());
        when(currentAccountPortIn.hasPermission("PAYMENT_CREATE_ALL")).thenReturn(true);
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(Set.of(
                OrganizationAccessGuard.PARKING_SCOPE_PARTNER)));
        when(organizationAccessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut))
                .thenReturn(Set.of(invoice.getParkingLotId()));

        assertDoesNotThrow(() -> guard.ensureCanCreateVnpayPayment(invoice));
        verify(organizationAccessGuard).resolveAccessibleParkingLotIds(parkingLotPortOut);
    }

    @Test
    void employeeCanRecordOnlyAtAnOpenShiftLot() {
        UUID shiftLot = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(account(
                accountId, Set.of("PARKING_SCOPE_SHIFT")));
        when(employeePaymentScopePortOut.findOpenShiftParkingLotIds(accountId)).thenReturn(Set.of(shiftLot));

        Invoice ownInvoice = new Invoice();
        ownInvoice.setParkingLotId(shiftLot);
        Invoice otherInvoice = new Invoice();
        otherInvoice.setParkingLotId(UUID.randomUUID());
        assertDoesNotThrow(() -> guard.ensureCanRecordPayment(ownInvoice));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanRecordPayment(otherInvoice));
    }

    private CurrentAccountAccess account(Set<String> permissions) {
        return account(UUID.randomUUID(), permissions);
    }

    private CurrentAccountAccess account(UUID accountId, Set<String> permissions) {
        return new CurrentAccountAccess(accountId, "subject", "user", "user@example.com",
                UUID.randomUUID(), "TEST_ROLE", AccountStatus.ACTIVE, null, permissions);
    }
}
