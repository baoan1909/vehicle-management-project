package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportRequesterContextResolver;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class SupportRequesterContextResolverTest {

    private final CurrentAccountPortIn currentAccountPortIn = mock(CurrentAccountPortIn.class);
    private final SupportRequesterContextResolver resolver = new SupportRequesterContextResolver(currentAccountPortIn);

    @Test
    void resolvesSupportedRequesterRoleAfterPermissionCheck() {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        CurrentAccountAccess access = access(accountId, "PARTNER_ADMIN", Set.of(organizationId), Set.of(parkingLotId));
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);

        SupportRequesterContext requester = resolver.resolve("SUPPORT_TICKET_CREATE_OWN");

        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_CREATE_OWN");
        assertThat(requester.accountId()).isEqualTo(accountId);
        assertThat(requester.roleCode()).isEqualTo("PARTNER_ADMIN");
        assertThat(requester.organizationIds()).containsExactly(organizationId);
        assertThat(requester.parkingLotIds()).containsExactly(parkingLotId);
    }

    @Test
    void doesNotAuthorizeByHardcodedRoleName() {
        CurrentAccountAccess access = access(
                UUID.randomUUID(), "CUSTOM_REQUESTER_ROLE", Set.of(), Set.of()
        );
        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(access);

        SupportRequesterContext requester = resolver.resolve("SUPPORT_TICKET_CREATE_OWN");

        assertThat(requester.roleCode()).isEqualTo("CUSTOM_REQUESTER_ROLE");
        verify(currentAccountPortIn).requirePermission("SUPPORT_TICKET_CREATE_OWN");
    }

    @Test
    void rejectsParkingLotOutsideRequesterScope() {
        SupportRequesterContext requester = new SupportRequesterContext(
                UUID.randomUUID(), "EMPLOYEE", "employee", "employee@example.com",
                Set.of("SUPPORT_TICKET_CREATE_OWN"), Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID())
        );

        assertThatThrownBy(() -> resolver.ensureScopeAllowed(requester, null, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Parking lot is outside");
    }

    private CurrentAccountAccess access(
            UUID accountId,
            String roleCode,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds
    ) {
        CurrentAccountAccess access = mock(CurrentAccountAccess.class);
        when(access.accountId()).thenReturn(accountId);
        when(access.roleCode()).thenReturn(roleCode);
        when(access.username()).thenReturn("requester");
        when(access.email()).thenReturn("requester@example.com");
        when(access.getEffectivePermissionCodes()).thenReturn(Set.of("SUPPORT_TICKET_CREATE_OWN"));
        when(access.activeOrganizationIds()).thenReturn(organizationIds);
        when(access.assignedParkingLotIds()).thenReturn(parkingLotIds);
        return access;
    }
}
