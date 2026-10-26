package com.ban.vehicle_management.infrastructure.realtime.parking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ParkingRealtimeSubscriptionGuardTest {
    private final ParkingLotPortOut parkingLotPortOut = mock(ParkingLotPortOut.class);
    private final ChatConversationPortOut chatConversationPortOut = mock(ChatConversationPortOut.class);
    private final ParkingRealtimeSubscriptionGuard guard =
            new ParkingRealtimeSubscriptionGuard(parkingLotPortOut, chatConversationPortOut);

    @Test
    void unknownAndMalformedDestinationsAreDenied() {
        CurrentAccountAccess actor = access("CUSTOM", Set.of("CHAT_CONVERSATION_READ_OWN"), Set.of(), Set.of());
        assertThrows(AccessDeniedException.class,
                () -> guard.authorizeSubscription(actor, "/topic/unknown"));
        assertThrows(AccessDeniedException.class,
                () -> guard.authorizeSubscription(actor, "/topic/parking-lots/not-a-uuid"));
        assertThrows(AccessDeniedException.class,
                () -> guard.authorizeSubscription(actor, "/user/queue/chat/conversations/not-a-uuid"));
    }

    @Test
    void parkingDestinationLoadsResourceAndAppliesCurrentScope() {
        UUID lotId = UUID.randomUUID();
        ParkingLot lot = lot(lotId, UUID.randomUUID());
        when(parkingLotPortOut.findById(lotId)).thenReturn(java.util.Optional.of(lot));
        CurrentAccountAccess actor = access("CUSTOM", Set.of(
                "PARKING_LOT_READ_ALL", "PARKING_SCOPE_ASSIGNED"), Set.of(), Set.of(lotId));

        assertDoesNotThrow(() -> guard.authorizeSubscription(
                actor, "/topic/parking-lots/" + lotId));
    }

    @Test
    void chatRequiresPermissionAndConversationRelationship() {
        UUID conversationId = UUID.randomUUID();
        CurrentAccountAccess actor = access("CUSTOM", Set.of("CHAT_CONVERSATION_READ_OWN"), Set.of(), Set.of());
        when(chatConversationPortOut.existsActiveMember(conversationId, actor.accountId())).thenReturn(true);

        assertDoesNotThrow(() -> guard.authorizeSubscription(actor, "/user/queue/chat"));
        assertDoesNotThrow(() -> guard.authorizeSubscription(
                actor, "/user/queue/chat/conversations/" + conversationId));

        CurrentAccountAccess other = access("CUSTOM", Set.of("CHAT_CONVERSATION_READ_OWN"), Set.of(), Set.of());
        assertThrows(AccessDeniedException.class, () -> guard.authorizeSubscription(
                other, "/user/queue/chat/conversations/" + conversationId));
        assertThrows(AccessDeniedException.class, () -> guard.authorizeSubscription(
                access("CUSTOM", Set.of(), Set.of(), Set.of()), "/user/topic/chat"));
    }

    @Test
    void pendingOnboardingActorCanReceiveOnlyIdentityBoundNotifications() {
        CurrentAccountAccess pending = new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "user", "user@example.com", UUID.randomUUID(),
                "CUSTOM", true, false, Set.of(), Set.of(), AccountStatus.PENDING,
                null, null, null, true, Set.of());
        assertDoesNotThrow(() -> guard.authorizeSubscription(pending, "/user/queue/notifications"));
        assertThrows(AccessDeniedException.class,
                () -> guard.authorizeSubscription(pending, "/queue/notifications"));
    }

    @Test
    void lockedActorCannotReceiveNotifications() {
        CurrentAccountAccess locked = new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "user", "user@example.com", UUID.randomUUID(),
                "CUSTOM", true, false, Set.of(), Set.of(), AccountStatus.LOCKED,
                null, null, null, false, Set.of());
        assertThrows(AccessDeniedException.class,
                () -> guard.authorizeSubscription(locked, "/user/queue/notifications"));
    }

    @Test
    void subscribeWithoutActionPermissionIsDenied() {
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanSubscribe(
                access("CUSTOM", Set.of("PARKING_SCOPE_PLATFORM"), Set.of(), Set.of()),
                lot(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void customRoleWithSamePermissionAndScopeBehavesLikeStandardRole() {
        UUID organizationId = UUID.randomUUID();
        Set<String> permissions = Set.of("PARKING_LOT_READ_ALL", "PARKING_SCOPE_PARTNER");
        ParkingLot lot = lot(UUID.randomUUID(), organizationId);
        assertDoesNotThrow(() -> guard.ensureCanSubscribe(
                access("PARTNER_ADMIN", permissions, Set.of(organizationId), Set.of()), lot));
        assertDoesNotThrow(() -> guard.ensureCanSubscribe(
                access("CUSTOM_PARTNER", permissions, Set.of(organizationId), Set.of()), lot));
    }

    @Test
    void partnerScopeCannotSubscribeToAnotherOrganization() {
        Set<String> permissions = Set.of("PARKING_LOT_READ_ALL", "PARKING_SCOPE_PARTNER");
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanSubscribe(
                access("CUSTOM_PARTNER", permissions, Set.of(UUID.randomUUID()), Set.of()),
                lot(UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void assignedScopeRequiresMatchingParkingLot() {
        UUID assignedLotId = UUID.randomUUID();
        Set<String> permissions = Set.of("PARKING_LOT_READ_ALL", "PARKING_SCOPE_ASSIGNED");
        assertDoesNotThrow(() -> guard.ensureCanSubscribe(
                access("CUSTOM_OPERATOR", permissions, Set.of(), Set.of(assignedLotId)),
                lot(assignedLotId, UUID.randomUUID())));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanSubscribe(
                access("CUSTOM_OPERATOR", permissions, Set.of(), Set.of(assignedLotId)),
                lot(UUID.randomUUID(), UUID.randomUUID())));
    }

    private CurrentAccountAccess access(
            String role, Set<String> permissions, Set<UUID> organizations, Set<UUID> lots) {
        return new CurrentAccountAccess(
                UUID.randomUUID(), "subject", "user", "user@example.com", UUID.randomUUID(),
                role, true, !organizations.isEmpty(), organizations, lots, AccountStatus.ACTIVE,
                lots.isEmpty() ? null
                        : com.ban.vehicle_management.shared.enumeration.people.EmployeeStatus.ACTIVE,
                null, null, false, permissions);
    }

    private ParkingLot lot(UUID lotId, UUID organizationId) {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(lotId);
        lot.setOrganizationId(organizationId);
        return lot;
    }
}
