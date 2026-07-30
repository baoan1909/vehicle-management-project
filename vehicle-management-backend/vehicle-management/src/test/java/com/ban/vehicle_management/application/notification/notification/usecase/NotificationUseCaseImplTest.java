package com.ban.vehicle_management.application.notification.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private NotificationPortOut notificationPortOut;

    @Mock
    private NotificationRealtimeEventPublisherPortOut realtimeEventPublisher;

    @Mock
    private NotificationRealtimeMessageMapper realtimeMessageMapper;

    private NotificationUseCaseImpl notificationUseCase;

    @BeforeEach
    void setUp() {
        notificationUseCase = new NotificationUseCaseImpl(
                currentAccountPortIn,
                notificationPortOut,
                realtimeEventPublisher,
                realtimeMessageMapper
        );
    }

    @Test
    void sendWebNotification_shouldPersistSentWebNotificationAndPublishRealtimeMessage() {
        UUID accountId = UUID.randomUUID();
        SendNotificationCommand command = new SendNotificationCommand(
                accountId,
                " Subscription approved ",
                " Your subscription is active ",
                "access_control",
                "subscriptions",
                UUID.randomUUID()
        );

        Notification savedNotification = new Notification();
        savedNotification.setNotificationId(UUID.randomUUID());
        savedNotification.setAccountId(accountId);
        savedNotification.setChannel(NotificationChannel.WEB);
        savedNotification.setTitle("Subscription approved");
        savedNotification.setMessage("Your subscription is active");
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setSentAt(Instant.now());
        NotificationRealtimeMessage realtimeMessage = new NotificationRealtimeMessage(
                savedNotification.getNotificationId(),
                accountId,
                NotificationChannel.WEB,
                savedNotification.getTitle(),
                savedNotification.getMessage(),
                NotificationStatus.SENT,
                "2026-07-30 10:00:00",
                null,
                "access_control",
                "subscriptions",
                command.relatedId(),
                "2026-07-30 10:00:00"
        );

        when(notificationPortOut.existsAccountById(accountId)).thenReturn(true);
        when(notificationPortOut.save(any(Notification.class))).thenReturn(savedNotification);
        when(realtimeMessageMapper.toRealtimeMessage(savedNotification)).thenReturn(realtimeMessage);

        Notification result = notificationUseCase.sendWebNotification(command);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationPortOut).save(notificationCaptor.capture());
        Notification notificationToSave = notificationCaptor.getValue();
        assertNotNull(notificationToSave.getNotificationId());
        assertEquals(accountId, notificationToSave.getAccountId());
        assertEquals(NotificationChannel.WEB, notificationToSave.getChannel());
        assertEquals(NotificationStatus.SENT, notificationToSave.getStatus());
        assertEquals("Subscription approved", notificationToSave.getTitle());
        assertEquals("Your subscription is active", notificationToSave.getMessage());
        assertNotNull(notificationToSave.getSentAt());
        verify(realtimeEventPublisher).publish(realtimeMessage);
        assertEquals(savedNotification, result);
    }

    @Test
    void markMyNotificationAsRead_shouldOnlyUpdateCurrentAccountNotification() {
        UUID accountId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setAccountId(accountId);
        notification.setStatus(NotificationStatus.SENT);

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
        when(notificationPortOut.findByIdAndAccountId(notificationId, accountId)).thenReturn(Optional.of(notification));
        when(notificationPortOut.save(notification)).thenReturn(notification);

        Notification result = notificationUseCase.markMyNotificationAsRead(notificationId);

        assertEquals(NotificationStatus.READ, result.getStatus());
        assertNotNull(result.getReadAt());
        verify(notificationPortOut).save(notification);
    }
}
