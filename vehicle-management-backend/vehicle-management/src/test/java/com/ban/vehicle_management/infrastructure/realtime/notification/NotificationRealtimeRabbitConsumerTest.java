package com.ban.vehicle_management.infrastructure.realtime.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

@ExtendWith(MockitoExtension.class)
class NotificationRealtimeRabbitConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpUserRegistry simpUserRegistry;

    @Mock
    private NotificationPortOut notificationPortOut;

    @Mock
    private SimpUser simpUser;

    @Mock
    private SimpSession simpSession;

    @Mock
    private SimpSubscription simpSubscription;

    private NotificationRealtimeRabbitConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new NotificationRealtimeRabbitConsumer(messagingTemplate, simpUserRegistry, notificationPortOut);
    }

    @Test
    void handle_shouldMarkRealtimeDeliveredWhenUserHasNotificationSubscription() {
        UUID accountId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationRealtimeMessage message = message(notificationId, accountId);

        when(simpUserRegistry.getUser(accountId.toString())).thenReturn(simpUser);
        when(simpUser.getSessions()).thenReturn(Set.of(simpSession));
        when(simpSession.getSubscriptions()).thenReturn(Set.of(simpSubscription));
        when(simpSubscription.getDestination()).thenReturn("/user/queue/notifications");

        consumer.handle(message);

        verify(messagingTemplate).convertAndSendToUser(accountId.toString(), "/queue/notifications", message);
        verify(notificationPortOut).markRealtimeDelivered(eq(notificationId), any(Instant.class));
    }

    @Test
    void handle_shouldNotMarkRealtimeDeliveredWhenUserHasNoSubscription() {
        UUID accountId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationRealtimeMessage message = message(notificationId, accountId);

        when(simpUserRegistry.getUser(accountId.toString())).thenReturn(null);

        consumer.handle(message);

        verify(messagingTemplate).convertAndSendToUser(accountId.toString(), "/queue/notifications", message);
        verify(notificationPortOut, never()).markRealtimeDelivered(eq(notificationId), any(Instant.class));
    }

    private NotificationRealtimeMessage message(UUID notificationId, UUID accountId) {
        return new NotificationRealtimeMessage(
                notificationId,
                accountId,
                NotificationChannel.WEB,
                "Title",
                "Message",
                NotificationStatus.SENT,
                "2026-07-30 10:00:00",
                null,
                "access_control",
                "subscriptions",
                UUID.randomUUID(),
                "2026-07-30 10:00:00"
        );
    }
}
