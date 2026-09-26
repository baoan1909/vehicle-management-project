package com.ban.vehicle_management.infrastructure.realtime.notification;

import com.ban.vehicle_management.application.notification.notification.config.NotificationTimeProperties;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import java.security.Principal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class NotificationSubscriptionSyncListener {

    private static final String QUEUE_SEND_DESTINATION = "/queue/notifications";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSubscriptionSyncListener.class);

    private final NotificationPortOut notificationPortOut;
    private final NotificationRealtimeMessageMapper realtimeMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final Duration pendingReplayWindow;
    private final Clock clock;

    @Autowired
    public NotificationSubscriptionSyncListener(
            NotificationPortOut notificationPortOut,
            NotificationRealtimeMessageMapper realtimeMessageMapper,
            SimpMessagingTemplate messagingTemplate,
            NotificationTimeProperties timeProperties,
            Clock clock
    ) {
        this(notificationPortOut, realtimeMessageMapper, messagingTemplate,
                timeProperties.getPendingReplayWindow(), clock);
    }

    public NotificationSubscriptionSyncListener(
            NotificationPortOut notificationPortOut,
            NotificationRealtimeMessageMapper realtimeMessageMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this(notificationPortOut, realtimeMessageMapper, messagingTemplate,
                Duration.ofHours(6), Clock.systemUTC());
    }

    private NotificationSubscriptionSyncListener(
            NotificationPortOut notificationPortOut,
            NotificationRealtimeMessageMapper realtimeMessageMapper,
            SimpMessagingTemplate messagingTemplate,
            Duration pendingReplayWindow,
            Clock clock
    ) {
        this.notificationPortOut = notificationPortOut;
        this.realtimeMessageMapper = realtimeMessageMapper;
        this.messagingTemplate = messagingTemplate;
        this.pendingReplayWindow = pendingReplayWindow;
        this.clock = clock;
    }

    @EventListener
    @Transactional
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        if (user == null
                || sessionId == null
                || !NotificationRealtimeRabbitConsumer.isNotificationDestination(destination)) {
            return;
        }

        UUID accountId = parseAccountId(user.getName());
        if (accountId == null) {
            LOGGER.warn("Skip notification replay because websocket principal is not an account UUID: {}", user.getName());
            return;
        }

        Instant now = Instant.now(clock);
        Instant replayCutoff = now.minus(pendingReplayWindow);
        List<Notification> pendingNotifications = notificationPortOut.findPendingRealtimeNotifications(
                accountId,
                replayCutoff
        );
        if (pendingNotifications.isEmpty()) {
            return;
        }

        MessageHeaders headers = createHeaders(sessionId);
        for (Notification notification : pendingNotifications) {
            NotificationRealtimeMessage payload = realtimeMessageMapper.toRealtimeMessage(notification);
            messagingTemplate.convertAndSendToUser(
                    user.getName(),
                    QUEUE_SEND_DESTINATION,
                    payload,
                    headers
            );
        }

        notificationPortOut.markRealtimeDelivered(
                pendingNotifications.stream()
                        .map(Notification::getNotificationId)
                        .toList(),
                now
        );
    }

    private UUID parseAccountId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
