package com.ban.vehicle_management.infrastructure.realtime.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.iam.account.port.out.AccountAuthorizationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.infrastructure.realtime.security.WebSocketActorPrincipal;
import com.ban.vehicle_management.infrastructure.realtime.parking.ParkingRealtimeSubscriptionGuard;
import com.ban.vehicle_management.infrastructure.security.jwt.JwtAuthenticationConverter;
import com.ban.vehicle_management.infrastructure.security.principal.AuthenticatedAccountPrincipal;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketConfigTest {
    @Mock JwtDecoder jwtDecoder;
    @Mock JwtAuthenticationConverter converter;
    @Mock AccountAuthorizationPortOut authorizationPort;
    @Mock ParkingRealtimeSubscriptionGuard destinationAccessGuard;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void connectWithoutTokenIsDenied() {
        ChatWebSocketConfig config = config();
        assertThrows(AccessDeniedException.class,
                () -> config.authenticate(StompHeaderAccessor.create(StompCommand.CONNECT)));
    }

    @Test
    void connectWithInvalidTokenIsDenied() {
        ChatWebSocketConfig config = config();
        StompHeaderAccessor accessor = connect("bad-token");
        when(jwtDecoder.decode("bad-token")).thenThrow(new IllegalArgumentException("invalid"));
        assertThrows(AccessDeniedException.class, () -> config.authenticate(accessor));
    }

    @Test
    void inactiveAccountConnectsWithoutBusinessAuthoritiesAndDoesNotTouchHttpContext() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess inactive = access(accountId, AccountStatus.LOCKED, Set.of("ACCOUNT_READ_ALL"));
        Jwt jwt = jwt(accountId);
        when(jwtDecoder.decode("token")).thenReturn(jwt);
        when(converter.convert(jwt)).thenReturn(authentication(inactive));

        Authentication result = config().authenticate(connect("token"));

        assertTrue(result.getAuthorities().isEmpty());
        WebSocketActorPrincipal actor = assertInstanceOf(
                WebSocketActorPrincipal.class, result.getPrincipal());
        assertEquals(inactive, actor.accessSnapshot());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void changedRevisionAfterConnectRejectsSensitiveFrame() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess connected = access(accountId, AccountStatus.ACTIVE, Set.of("ACCOUNT_READ_ALL"));
        CurrentAccountAccess revoked = access(accountId, AccountStatus.ACTIVE, Set.of());
        StompHeaderAccessor frame = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        frame.setUser(websocketAuthentication(connected));
        when(authorizationPort.findByAccountId(accountId)).thenReturn(Optional.of(revoked));

        assertThrows(AccessDeniedException.class, () -> config().revalidate(frame));
    }

    @Test
    void accountLockAfterConnectRejectsSensitiveFrame() {
        assertSensitiveFrameRejectedAfterAccessChange(access -> copy(access, AccountStatus.LOCKED,
                access.roleActive(), access.hasActivePartnerOrganizationMembership(),
                access.activeOrganizationIds(), access.assignedParkingLotIds(), access.permissionCodes()));
    }

    @Test
    void roleDisableAfterConnectRejectsSensitiveFrame() {
        assertSensitiveFrameRejectedAfterAccessChange(access -> copy(access, access.status(), false,
                access.hasActivePartnerOrganizationMembership(), access.activeOrganizationIds(),
                access.assignedParkingLotIds(), access.permissionCodes()));
    }

    @Test
    void partnerMembershipRemovalAfterConnectRejectsSensitiveFrame() {
        UUID organizationId = UUID.randomUUID();
        CurrentAccountAccess connected = copy(
                access(UUID.randomUUID(), AccountStatus.ACTIVE, Set.of("PARKING_SCOPE_PARTNER")),
                AccountStatus.ACTIVE, true, true, Set.of(organizationId), Set.of(),
                Set.of("PARKING_SCOPE_PARTNER"));
        assertSensitiveFrameRejectedAfterAccessChange(connected, access -> copy(access, access.status(),
                access.roleActive(), false, Set.of(), access.assignedParkingLotIds(), access.permissionCodes()));
    }

    @Test
    void parkingAssignmentRemovalAfterConnectRejectsSensitiveFrame() {
        UUID parkingLotId = UUID.randomUUID();
        CurrentAccountAccess connected = copy(
                access(UUID.randomUUID(), AccountStatus.ACTIVE, Set.of("PARKING_SCOPE_ASSIGNED")),
                AccountStatus.ACTIVE, true, false, Set.of(), Set.of(parkingLotId),
                Set.of("PARKING_SCOPE_ASSIGNED"));
        assertSensitiveFrameRejectedAfterAccessChange(connected, access -> copy(access, access.status(),
                access.roleActive(), access.hasActivePartnerOrganizationMembership(),
                access.activeOrganizationIds(), Set.of(), access.permissionCodes()));
    }

    @Test
    void subscribeRevalidatesExactlyOnceAndAuthorizesUsingFreshSnapshot() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess connected = access(accountId, AccountStatus.ACTIVE, Set.of("ACCOUNT_READ_ALL"));
        StompHeaderAccessor frame = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        frame.setDestination("/user/queue/notifications");
        frame.setUser(websocketAuthentication(connected));
        when(authorizationPort.findByAccountId(accountId)).thenReturn(Optional.of(connected));

        config().authorizeFrame(frame);

        verify(authorizationPort).findByAccountId(accountId);
        verify(destinationAccessGuard).authorizeSubscription(connected, "/user/queue/notifications");
    }

    @Test
    void clientSendIsDeniedAfterSingleRevalidation() {
        UUID accountId = UUID.randomUUID();
        CurrentAccountAccess connected = access(accountId, AccountStatus.ACTIVE, Set.of("ACCOUNT_READ_ALL"));
        StompHeaderAccessor frame = StompHeaderAccessor.create(StompCommand.SEND);
        frame.setDestination("/topic/parking-lots/" + UUID.randomUUID());
        frame.setUser(websocketAuthentication(connected));
        when(authorizationPort.findByAccountId(accountId)).thenReturn(Optional.of(connected));

        assertThrows(AccessDeniedException.class, () -> config().authorizeFrame(frame));

        verify(authorizationPort).findByAccountId(accountId);
        verifyNoInteractions(destinationAccessGuard);
    }

    @Test
    void twoSessionsRetainDistinctImmutableActors() {
        CurrentAccountAccess first = access(UUID.randomUUID(), AccountStatus.ACTIVE, Set.of("A"));
        CurrentAccountAccess second = access(UUID.randomUUID(), AccountStatus.ACTIVE, Set.of("B"));
        Authentication firstSession = websocketAuthentication(first);
        Authentication secondSession = websocketAuthentication(second);

        assertEquals(first.accountId(), ((WebSocketActorPrincipal) firstSession.getPrincipal()).accountId());
        assertEquals(second.accountId(), ((WebSocketActorPrincipal) secondSession.getPrincipal()).accountId());
        assertNotEquals(firstSession.getPrincipal(), secondSession.getPrincipal());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private ChatWebSocketConfig config() {
        return new ChatWebSocketConfig(jwtDecoder, converter, authorizationPort, destinationAccessGuard);
    }

    private StompHeaderAccessor connect(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        return accessor;
    }

    private UsernamePasswordAuthenticationToken authentication(CurrentAccountAccess access) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("jwt", "", List.of());
        authentication.setDetails(new AuthenticatedAccountPrincipal(
                access.accountId(), access.subject(), access.username(), access.email(), access));
        return authentication;
    }

    private UsernamePasswordAuthenticationToken websocketAuthentication(CurrentAccountAccess access) {
        WebSocketActorPrincipal actor = new WebSocketActorPrincipal(
                access.accountId(), access, access.accessRevision());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(actor, "", List.of());
        authentication.setDetails(actor);
        return authentication;
    }

    private CurrentAccountAccess access(UUID accountId, AccountStatus status, Set<String> permissions) {
        return new CurrentAccountAccess(
                accountId, "subject-" + accountId, "user", "user@example.com", UUID.randomUUID(),
                "CUSTOM", true, false, Set.of(), Set.of(), status, null, null, null, false, permissions);
    }

    private void assertSensitiveFrameRejectedAfterAccessChange(
            java.util.function.UnaryOperator<CurrentAccountAccess> stateChange
    ) {
        assertSensitiveFrameRejectedAfterAccessChange(
                access(UUID.randomUUID(), AccountStatus.ACTIVE, Set.of("ACCOUNT_READ_ALL")), stateChange);
    }

    private void assertSensitiveFrameRejectedAfterAccessChange(
            CurrentAccountAccess connected,
            java.util.function.UnaryOperator<CurrentAccountAccess> stateChange
    ) {
        StompHeaderAccessor frame = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        frame.setUser(websocketAuthentication(connected));
        when(authorizationPort.findByAccountId(connected.accountId()))
                .thenReturn(Optional.of(stateChange.apply(connected)));

        assertThrows(AccessDeniedException.class, () -> config().revalidate(frame));
        verify(authorizationPort).findByAccountId(connected.accountId());
    }

    private CurrentAccountAccess copy(
            CurrentAccountAccess source,
            AccountStatus status,
            boolean roleActive,
            boolean partnerMembership,
            Set<UUID> organizationIds,
            Set<UUID> parkingLotIds,
            Set<String> permissions
    ) {
        return new CurrentAccountAccess(
                source.accountId(), source.subject(), source.username(), source.email(), source.roleId(),
                source.roleCode(), roleActive, partnerMembership, organizationIds, parkingLotIds, status,
                source.employeeStatus(), source.customerStatus(), source.customerApprovalStatus(),
                source.onboardingRequired(), permissions);
    }

    private Jwt jwt(UUID accountId) {
        Instant now = Instant.now();
        return new Jwt("token", now, now.plusSeconds(60), java.util.Map.of("alg", "none"),
                java.util.Map.of("sub", "subject-" + accountId));
    }
}
