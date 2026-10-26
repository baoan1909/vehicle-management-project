package com.ban.vehicle_management.application.people.customer.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.exception.CustomerIdentityRequiredException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CustomerIdentityResolverTest {

    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock CustomerPortOut customerPortOut;
    private CustomerIdentityResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CustomerIdentityResolver(currentAccountPortIn, customerPortOut);
    }

    @Test
    void pendingAndRejectedCustomersCanResolveSupportIdentityWithNarrowQuery() {
        UUID accountId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                accountId, CustomerStatus.INACTIVE, CustomerApprovalStatus.PENDING));
        when(customerPortOut.findCustomerIdByAccountId(accountId)).thenReturn(Optional.of(customerId));

        assertEquals(customerId, resolver.resolveCurrentCustomerIdForSupport("SUPPORT_TICKET_READ_OWN"));
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_READ_OWN");

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                accountId, CustomerStatus.INACTIVE, CustomerApprovalStatus.REJECTED));
        assertEquals(customerId, resolver.resolveCurrentCustomerIdForSupport("SUPPORT_TICKET_READ_OWN"));
    }

    @Test
    void pendingCustomerCannotResolveApprovedIdentity() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                accountId, CustomerStatus.INACTIVE, CustomerApprovalStatus.PENDING));

        assertThrows(AccessDeniedException.class, resolver::resolveCurrentApprovedCustomerId);
        verify(customerPortOut, never()).findCustomerIdByAccountId(accountId);
    }

    @Test
    void missingCustomerRecordHasStableBusinessError() {
        UUID accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access(
                accountId, CustomerStatus.INACTIVE, CustomerApprovalStatus.REJECTED));
        when(customerPortOut.findCustomerIdByAccountId(accountId)).thenReturn(Optional.empty());

        assertThrows(CustomerIdentityRequiredException.class,
                () -> resolver.resolveCurrentCustomerIdForSupport("SUPPORT_WIDGET_ACCESS_OWN"));
    }

    private CurrentAccountAccess access(
            UUID accountId,
            CustomerStatus customerStatus,
            CustomerApprovalStatus approvalStatus
    ) {
        return new CurrentAccountAccess(accountId, "subject", "user", "user@example.com", UUID.randomUUID(),
                "CUSTOM_SUPPORT_ROLE", true, AccountStatus.ACTIVE, null, customerStatus, approvalStatus,
                Set.of("SUPPORT_TICKET_READ_OWN", "SUPPORT_WIDGET_ACCESS_OWN"));
    }
}
