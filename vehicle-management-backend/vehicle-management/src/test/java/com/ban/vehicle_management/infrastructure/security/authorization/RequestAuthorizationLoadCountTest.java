package com.ban.vehicle_management.infrastructure.security.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.security.adapter.CurrentAccountSecurityAdapter;
import com.ban.vehicle_management.infrastructure.security.assistant.AssistantActorScope;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RequestAuthorizationLoadCountTest {

    @Mock
    private AccountAuthorizationPortOut accountAuthorizationPortOut;

    @Mock
    private AssistantActorScope assistantActorScope;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtThenCurrentAccessLoadsAggregateOnce() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess access = access(accountId);
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(Optional.of(access));

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(accountAuthorizationPortOut);
        CurrentAccountSecurityAdapter adapter = new CurrentAccountSecurityAdapter(
                accountAuthorizationPortOut,
                assistantActorScope
        );

        SecurityContextHolder.getContext().setAuthentication(converter.convert(jwt(accountId)));

        assertEquals(accountId, adapter.getCurrentAccountOrThrow().accountId());
        verify(accountAuthorizationPortOut).findByAccountId(accountId);
    }

    @Test
    void preAuthorizeThenUseCaseReadsLoadAggregateOnce() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess access = access(accountId);
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(Optional.of(access));
        CurrentAccountSecurityAdapter adapter = adapterWithLegacyAuthentication(accountId);
        PermissionAuthorizer authorizer = new PermissionAuthorizer(adapter);

        authorizer.hasPermission("ACCOUNT_READ_ALL");
        adapter.getCurrentAccountOrThrow();
        adapter.getCurrentAccountOrThrow();

        verify(accountAuthorizationPortOut).findByAccountId(accountId);
    }

    @Test
    void hasAnyPermissionResolvesAggregateOnce() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess access = access(accountId);
        when(accountAuthorizationPortOut.findByAccountId(accountId)).thenReturn(Optional.of(access));
        CurrentAccountSecurityAdapter adapter = adapterWithLegacyAuthentication(accountId);
        PermissionAuthorizer authorizer = new PermissionAuthorizer(adapter);

        authorizer.hasAnyPermission("MISSING_A", "MISSING_B", "ACCOUNT_READ_ALL");

        verify(accountAuthorizationPortOut).findByAccountId(accountId);
    }

    @Test
    void consecutiveRequestsEachReloadAndSecondRequestSeesRevocation() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess granted = access(accountId);
        CurrentAccountAccess revoked = new CurrentAccountAccess(
                accountId, granted.subject(), granted.username(), granted.email(), granted.roleId(),
                granted.roleCode(), AccountStatus.ACTIVE, null, Set.of());
        when(accountAuthorizationPortOut.findByAccountId(accountId))
                .thenReturn(Optional.of(granted), Optional.of(revoked));

        CurrentAccountSecurityAdapter firstRequest = adapterWithLegacyAuthentication(accountId);
        assertTrue(new PermissionAuthorizer(firstRequest).hasPermission("ACCOUNT_READ_ALL"));

        SecurityContextHolder.clearContext();
        CurrentAccountSecurityAdapter secondRequest = adapterWithLegacyAuthentication(accountId);
        assertFalse(new PermissionAuthorizer(secondRequest).hasPermission("ACCOUNT_READ_ALL"));

        verify(accountAuthorizationPortOut, times(2)).findByAccountId(accountId);
    }

    @Test
    void explicitInvalidationRefreshesSnapshotWithinSameRequest() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess granted = access(accountId);
        CurrentAccountAccess revoked = new CurrentAccountAccess(
                accountId, granted.subject(), granted.username(), granted.email(), granted.roleId(),
                granted.roleCode(), AccountStatus.ACTIVE, null, Set.of());
        when(accountAuthorizationPortOut.findByAccountId(accountId))
                .thenReturn(Optional.of(granted), Optional.of(revoked));
        CurrentAccountSecurityAdapter adapter = adapterWithLegacyAuthentication(accountId);

        assertEquals(granted, adapter.getCurrentAccountOrThrow());
        adapter.invalidateCurrentAccountSnapshot();
        assertEquals(revoked, adapter.refreshCurrentAccountOrThrow());

        verify(accountAuthorizationPortOut, times(2)).findByAccountId(accountId);
    }

    private CurrentAccountSecurityAdapter adapterWithLegacyAuthentication(UUID accountId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "legacy-user",
                "credentials",
                List.of()
        );
        authentication.setDetails(new AuthenticatedAccountPrincipal(
                accountId,
                "subject-" + accountId,
                "user-" + accountId,
                "user-" + accountId + "@example.com"
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return new CurrentAccountSecurityAdapter(accountAuthorizationPortOut, assistantActorScope);
    }

    private CurrentAccountAccess access(UUID accountId) {
        return new CurrentAccountAccess(
                accountId,
                "subject-" + accountId,
                "user-" + accountId,
                "user-" + accountId + "@example.com",
                UUID.randomUUID(),
                "CUSTOM_ACCESS_ROLE",
                AccountStatus.ACTIVE,
                null,
                Set.of("ACCOUNT_READ_ALL")
        );
    }

    private Jwt jwt(UUID accountId) {
        Instant issuedAt = Instant.now();
        return new Jwt(
                "token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "subject-" + accountId,
                        "account_id", accountId.toString(),
                        "preferred_username", "user-" + accountId
                )
        );
    }
}
