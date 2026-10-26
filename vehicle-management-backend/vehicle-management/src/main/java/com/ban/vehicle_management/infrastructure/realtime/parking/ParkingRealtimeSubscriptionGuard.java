package com.ban.vehicle_management.infrastructure.realtime.parking;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Deny-by-default authorization matrix for every client STOMP subscription. */
@Component
public class ParkingRealtimeSubscriptionGuard {

    private static final String LOT_TOPIC_PREFIX = "/topic/parking-lots/";
    private static final String CHAT_QUEUE = "/user/queue/chat";
    private static final String CHAT_TOPIC = "/user/topic/chat";
    private static final String CHAT_CONVERSATION_PREFIX = "/user/queue/chat/conversations/";
    private static final Set<String> NOTIFICATION_DESTINATIONS = Set.of(
            "/user/queue/notifications",
            "/user/topic/notifications"
    );
    private static final Set<String> CHAT_READ_PERMISSIONS = Set.of(
            "CHAT_CONVERSATION_READ_ALL",
            "CHAT_CONVERSATION_READ_OWN"
    );
    private static final Set<String> READ_PERMISSIONS = Set.of(
            "PARKING_LOT_READ_ALL",
            "PARKING_SPACE_READ_ALL",
            "PARKING_SESSION_READ_ALL",
            "PARKING_SESSION_CHECK_IN_ALL",
            "PARKING_SESSION_CHECK_OUT_ALL"
    );

    private final ParkingLotPortOut parkingLotPortOut;
    private final ChatConversationPortOut chatConversationPortOut;

    public ParkingRealtimeSubscriptionGuard(
            ParkingLotPortOut parkingLotPortOut,
            ChatConversationPortOut chatConversationPortOut
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.chatConversationPortOut = chatConversationPortOut;
    }

    public void authorizeSubscription(CurrentAccountAccess actor, String destination) {
        if (actor == null || destination == null || destination.isBlank()) {
            deny();
        }
        if (destination.startsWith(LOT_TOPIC_PREFIX)) {
            authorizeParkingSubscription(actor, destination);
            return;
        }
        if (CHAT_QUEUE.equals(destination) || CHAT_TOPIC.equals(destination)) {
            ensureHasAnyPermission(actor, CHAT_READ_PERMISSIONS);
            return;
        }
        if (destination.startsWith(CHAT_CONVERSATION_PREFIX)) {
            authorizeConversationSubscription(actor, destination);
            return;
        }
        if (NOTIFICATION_DESTINATIONS.contains(destination)) {
            authorizeNotificationSubscription(actor);
            return;
        }
        deny();
    }

    private void authorizeParkingSubscription(CurrentAccountAccess actor, String destination) {
        String lotIdText = destination.substring(LOT_TOPIC_PREFIX.length());
        if (lotIdText.isBlank() || lotIdText.contains("/")) {
            deny();
        }
        try {
            UUID lotId = UUID.fromString(lotIdText);
            ParkingLot parkingLot = parkingLotPortOut.findById(lotId)
                    .orElseThrow(ParkingRealtimeSubscriptionGuard::denied);
            ensureCanSubscribe(actor, parkingLot);
        } catch (IllegalArgumentException exception) {
            deny();
        }
    }

    private void authorizeConversationSubscription(CurrentAccountAccess actor, String destination) {
        ensureHasAnyPermission(actor, CHAT_READ_PERMISSIONS);
        String conversationIdText = destination.substring(CHAT_CONVERSATION_PREFIX.length());
        if (conversationIdText.isBlank() || conversationIdText.contains("/")) {
            deny();
        }
        try {
            UUID conversationId = UUID.fromString(conversationIdText);
            if (!actor.getEffectivePermissionCodes().contains("CHAT_CONVERSATION_READ_ALL")
                    && !chatConversationPortOut.existsActiveMember(conversationId, actor.accountId())) {
                deny();
            }
        } catch (IllegalArgumentException exception) {
            deny();
        }
    }

    private void authorizeNotificationSubscription(CurrentAccountAccess actor) {
        if (!actor.roleActive()
                || !(com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.ACTIVE.equals(actor.status())
                || com.ban.vehicle_management.shared.enumeration.iam.AccountStatus.PENDING.equals(actor.status()))) {
            deny();
        }
    }

    private void ensureHasAnyPermission(CurrentAccountAccess actor, Set<String> requiredPermissions) {
        if (actor.getEffectivePermissionCodes().stream().noneMatch(requiredPermissions::contains)) {
            deny();
        }
    }

    void ensureCanSubscribe(CurrentAccountAccess actor, ParkingLot parkingLot) {
        Set<String> permissions = actor.getEffectivePermissionCodes();
        if (permissions.stream().noneMatch(READ_PERMISSIONS::contains)) {
            throw new AccessDeniedException("Access is denied");
        }
        if (permissions.contains("PARKING_SCOPE_PLATFORM")) {
            return;
        }
        if (permissions.contains("PARKING_SCOPE_PARTNER")
                && actor.activeOrganizationIds().contains(parkingLot.getOrganizationId())) {
            return;
        }
        if (permissions.contains("PARKING_SCOPE_ASSIGNED")
                && actor.assignedParkingLotIds().contains(parkingLot.getParkingLotId())) {
            return;
        }
        throw new AccessDeniedException("Access is denied");
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Access is denied");
    }

    private static void deny() {
        throw denied();
    }
}
