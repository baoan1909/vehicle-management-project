package com.ban.vehicle_management.infrastructure.security.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentAccountSecurityAdapterTest {

    @Mock
    private AccountAuthorizationPortOut accountAuthorizationPortOut;

    @InjectMocks
    private CurrentAccountSecurityAdapter currentAccountSecurityAdapter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldEnrichAuthenticationDetailsWithAccountIdWhenResolvedByKeycloakUserId() {
        UUID accountId = UUID.fromString("6d2ddc6a-bf89-47bc-94a7-44f446f37384");
        String keycloakUserId = "keycloak-user-id-001";

        CurrentAccountAccess currentAccountAccess = new CurrentAccountAccess(
                accountId,
                keycloakUserId,
                "baoan3236",
                "baoan3236@gmail.com",
                UUID.fromString("3e899a68-84de-4665-8fda-f1e39efc3346"),
                "SYSTEM_ADMIN",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_PROFILE_WRITE")
        );
        when(accountAuthorizationPortOut.findByKeycloakUserId(keycloakUserId))
                .thenReturn(Optional.of(currentAccountAccess));

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user",
                "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                null,
                keycloakUserId,
                "baoan3236",
                "baoan3236@gmail.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        UUID resolvedAccountId = currentAccountSecurityAdapter.getCurrentAccountIdOrThrow();
        assertEquals(accountId, resolvedAccountId);

        Object updatedDetails = authenticationToken.getDetails();
        AuthenticatedAccountPrincipal updatedPrincipal = assertInstanceOf(
                AuthenticatedAccountPrincipal.class,
                updatedDetails
        );
        assertEquals(accountId, updatedPrincipal.accountId());
    }

    @Test
    void shouldDenyBusinessPermissionForPendingInternalEmployee() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess currentAccountAccess = new CurrentAccountAccess(
                accountId,
                "keycloak-sub",
                "pending.employee",
                "pending.employee@example.com",
                UUID.randomUUID(),
                "EMPLOYEE",
                AccountStatus.ACTIVE,
                EmployeeStatus.INACTIVE,
                Set.of("EMPLOYEE_READ_ALL")
        );
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(Optional.of(currentAccountAccess));

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                "user",
                "pass",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        authenticationToken.setDetails(new AuthenticatedAccountPrincipal(
                accountId,
                "keycloak-sub",
                "pending.employee",
                "pending.employee@example.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        assertFalse(currentAccountSecurityAdapter.hasPermission("EMPLOYEE_READ_ALL"));
    }
}

