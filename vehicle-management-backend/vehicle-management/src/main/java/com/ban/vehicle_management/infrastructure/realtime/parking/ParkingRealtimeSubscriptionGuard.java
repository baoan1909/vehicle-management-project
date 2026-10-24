package com.ban.vehicle_management.infrastructure.realtime.parking;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.realtime.security.WebSocketActorPrincipal;
import java.security.Principal;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class ParkingRealtimeSubscriptionGuard implements WebSocketMessageBrokerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingRealtimeSubscriptionGuard.class);
    private static final String LOT_TOPIC_PREFIX = "/topic/parking-lots/";
    private static final Set<String> READ_PERMISSIONS = Set.of(
            "PARKING_LOT_READ_ALL",
            "PARKING_SPACE_READ_ALL",
            "PARKING_SESSION_READ_ALL",
            "PARKING_SESSION_CHECK_IN_ALL",
            "PARKING_SESSION_CHECK_OUT_ALL"
    );

    private final ParkingLotPortOut parkingLotPortOut;

    public ParkingRealtimeSubscriptionGuard(
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);
                if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    return message;
                }
                String destination = accessor.getDestination();
                if (destination == null || !destination.startsWith(LOT_TOPIC_PREFIX)) {
                    return message;
                }
                String lotIdText = destination.substring(LOT_TOPIC_PREFIX.length());
                int slash = lotIdText.indexOf('/');
                if (slash >= 0) {
                    lotIdText = lotIdText.substring(0, slash);
                }
                try {
                    UUID lotId = UUID.fromString(lotIdText);
                    CurrentAccountAccess actor = resolveActor(accessor.getUser());
                    ParkingLot parkingLot = parkingLotPortOut.findById(lotId)
                            .orElseThrow(() -> new AccessDeniedException("Access is denied"));
                    ensureCanSubscribe(actor, parkingLot);
                } catch (IllegalArgumentException malformed) {
                    throw new AccessDeniedException("Access is denied");
                } catch (RuntimeException denied) {
                    LOGGER.warn("Denied parking realtime subscription to {}", destination);
                    throw new AccessDeniedException("Access is denied");
                }
                return message;
            }
        });
    }

    CurrentAccountAccess resolveActor(Principal user) {
        if (user instanceof Authentication authentication) {
            Object details = authentication.getDetails();
            if (details instanceof WebSocketActorPrincipal actor) {
                return actor.accessSnapshot();
            }
            if (authentication.getPrincipal() instanceof WebSocketActorPrincipal actor) {
                return actor.accessSnapshot();
            }
        }
        if (user instanceof WebSocketActorPrincipal actor) {
            return actor.accessSnapshot();
        }
        throw new AccessDeniedException("Access is denied");
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
}
