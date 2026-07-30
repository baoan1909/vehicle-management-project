package com.ban.vehicle_management.infrastructure.realtime.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@ExtendWith(MockitoExtension.class)
class NotificationSubscriptionSyncListenerTest {

    @Mock
    private NotificationPortOut notificationPortOut;

    @Mock
    private NotificationRealtimeMessageMapper realtimeMessageMapper;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationSubscriptionSyncListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationSubscriptionSyncListener(
                notificationPortOut,
                realtimeMessageMapper,
                messagingTemplate
        );
    }

    @Test
    void onSubscribe_shouldReplayPendingNotificationsForSubscribedAccountSession() {
        UUID accountId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setAccountId(accountId);
        NotificationRealtimeMessage payload = new NotificationRealtimeMessage(
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

        when(notificationPortOut.findPendingRealtimeNotifications(eq(accountId), any(Instant.class)))
                .thenReturn(List.of(notification));
        when(realtimeMessageMapper.toRealtimeMessage(notification)).thenReturn(payload);

        listener.onSubscribe(createSubscribeEvent(accountId.toString(), "session-1", "/user/queue/notifications"));

        ArgumentCaptor<Map<String, Object>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(accountId.toString()),
                eq("/queue/notifications"),
                eq(payload),
                headersCaptor.capture()
        );
        assertEquals("session-1", headersCaptor.getValue().get(SimpMessageHeaderAccessor.SESSION_ID_HEADER));
        verify(notificationPortOut).markRealtimeDelivered(eq(List.of(notificationId)), any(Instant.class));
    }

    @Test
    void onSubscribe_shouldIgnoreNonNotificationDestination() {
        listener.onSubscribe(createSubscribeEvent(UUID.randomUUID().toString(), "session-1", "/user/queue/chat"));

        verify(notificationPortOut, never()).findPendingRealtimeNotifications(any(), any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(), any(Map.class));
    }

    private SessionSubscribeEvent createSubscribeEvent(String userName, String sessionId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(() -> userName);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        return new SessionSubscribeEvent(new ApplicationEventPublisher() {
            @Override
            public void publishEvent(Object event) {
                // no-op
            }
        }, message);
    }
}
