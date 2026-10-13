package com.ban.vehicle_management.infrastructure.realtime.parking;

import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
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
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
public class ParkingRealtimeSubscriptionGuard implements WebSocketMessageBrokerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParkingRealtimeSubscriptionGuard.class);
    private static final String LOT_TOPIC_PREFIX = "/topic/parking-lots/";

    private final ParkingLotPortOut parkingLotPortOut;
    private final OrganizationAccessGuard organizationAccessGuard;

    public ParkingRealtimeSubscriptionGuard(
            ParkingLotPortOut parkingLotPortOut,
            OrganizationAccessGuard organizationAccessGuard
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.organizationAccessGuard = organizationAccessGuard;
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
                    organizationAccessGuard.ensureCanAccessParkingLot(
                            parkingLotPortOut.findById(lotId).orElse(null));
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
}
