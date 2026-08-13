package com.ban.vehicle_management.application.notification.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationCommandMapper;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationRealtimeMessageMapper;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private BroadcastAnnouncementPortOut broadcastAnnouncementPortOut;

    @Mock
    private NotificationRealtimeEventPublisherPortOut realtimeEventPublisher;

    @Mock
    private NotificationRealtimeMessageMapper realtimeMessageMapper;

    @Mock
    private NotificationCommandMapper notificationCommandMapper;

    private NotificationUseCaseImpl notificationUseCase;

    @BeforeEach
    void setUp() {
        notificationUseCase = new NotificationUseCaseImpl(
                currentAccountPortIn,
                notificationPortOut,
                broadcastAnnouncementPortOut,
                realtimeEventPublisher,
                realtimeMessageMapper,
                notificationCommandMapper
        );
    }

    @Test
    void sendWebNotification_shouldPersistSentWebNotificationAndPublishRealtimeMessage() {
        UUID accountId = UUID.randomUUID();
        SendNotificationCommand command = new SendNotificationCommand(
                accountId,
                NotificationType.SUBSCRIPTION_APPROVED,
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
        savedNotification.setNotificationType(NotificationType.SUBSCRIPTION_APPROVED);
        savedNotification.setTitle("Subscription approved");
        savedNotification.setMessage("Your subscription is active");
        savedNotification.setStatus(NotificationStatus.SENT);
        savedNotification.setSentAt(Instant.now());
        NotificationRealtimeMessage realtimeMessage = new NotificationRealtimeMessage(
                savedNotification.getNotificationId(),
                accountId,
                null,
                NotificationChannel.WEB,
                NotificationType.SUBSCRIPTION_APPROVED,
                savedNotification.getTitle(),
                savedNotification.getMessage(),
                NotificationStatus.SENT,
                "2026-07-30 10:00:00",
                null,
                null,
                "access_control",
                "subscriptions",
                command.relatedId(),
                "2026-07-30 10:00:00"
        );

        when(notificationPortOut.existsAccountById(accountId)).thenReturn(true);
        when(notificationCommandMapper.toDomain(any(SendNotificationCommand.class))).thenAnswer(invocation -> {
            SendNotificationCommand normalizedCommand = invocation.getArgument(0);
            Notification notification = new Notification();
            notification.setAccountId(normalizedCommand.accountId());
            notification.setNotificationType(normalizedCommand.notificationType());
            notification.setTitle(normalizedCommand.title());
            notification.setMessage(normalizedCommand.message());
            notification.setRelatedSchema(normalizedCommand.relatedSchema());
            notification.setRelatedTable(normalizedCommand.relatedTable());
            notification.setRelatedId(normalizedCommand.relatedId());
            return notification;
        });
        when(notificationPortOut.save(any(Notification.class))).thenReturn(savedNotification);
        when(realtimeMessageMapper.toRealtimeMessage(savedNotification)).thenReturn(realtimeMessage);

        Notification result = notificationUseCase.sendWebNotification(command);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationPortOut).save(notificationCaptor.capture());
        Notification notificationToSave = notificationCaptor.getValue();
        assertNotNull(notificationToSave.getNotificationId());
        assertEquals(accountId, notificationToSave.getAccountId());
        assertEquals(NotificationChannel.WEB, notificationToSave.getChannel());
        assertEquals(NotificationType.SUBSCRIPTION_APPROVED, notificationToSave.getNotificationType());
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

    @Test
    void sendBroadcastWebNotification_shouldFanOutToResolvedActiveRecipientsAndPublishEachNotification() {
        UUID firstAccountId = UUID.randomUUID();
        UUID secondAccountId = UUID.randomUUID();
        UUID relatedId = UUID.randomUUID();
        BroadcastNotificationCommand command = new BroadcastNotificationCommand(
                false,
                Set.of("customer"),
                Set.of(firstAccountId),
                null,
                NotificationType.PARKING_LOT_MAINTENANCE,
                " Maintenance notice ",
                " Parking lot is under maintenance ",
                null,
                "parking",
                "parking_lots",
                relatedId
        );

        Notification firstNotification = notification(firstAccountId, "Maintenance notice");
        Notification secondNotification = notification(secondAccountId, "Maintenance notice");
        Notification savedFirstNotification = notification(firstAccountId, "Maintenance notice");
        savedFirstNotification.setNotificationId(UUID.randomUUID());
        Notification savedSecondNotification = notification(secondAccountId, "Maintenance notice");
        savedSecondNotification.setNotificationId(UUID.randomUUID());
        NotificationRealtimeMessage firstRealtimeMessage = realtimeMessage(savedFirstNotification);
        NotificationRealtimeMessage secondRealtimeMessage = realtimeMessage(savedSecondNotification);

        when(notificationPortOut.findActiveAccountIdsForBroadcast(
                false,
                List.of("CUSTOMER"),
                List.of(firstAccountId)
        )).thenReturn(List.of(firstAccountId, secondAccountId));
        when(notificationCommandMapper.toDomain(any(BroadcastNotificationCommand.class), eq(firstAccountId)))
                .thenReturn(firstNotification);
        when(notificationCommandMapper.toDomain(any(BroadcastNotificationCommand.class), eq(secondAccountId)))
                .thenReturn(secondNotification);
        when(notificationPortOut.saveAll(anyList()))
                .thenReturn(List.of(savedFirstNotification, savedSecondNotification));
        when(realtimeMessageMapper.toRealtimeMessage(savedFirstNotification)).thenReturn(firstRealtimeMessage);
        when(realtimeMessageMapper.toRealtimeMessage(savedSecondNotification)).thenReturn(secondRealtimeMessage);

        List<Notification> result = notificationUseCase.sendBroadcastWebNotification(command);

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationPortOut).saveAll(notificationsCaptor.capture());
        List<Notification> notificationsToSave = notificationsCaptor.getValue();
        assertEquals(2, notificationsToSave.size());
        notificationsToSave.forEach(notification -> {
            assertNotNull(notification.getNotificationId());
            assertEquals(NotificationChannel.WEB, notification.getChannel());
            assertEquals(NotificationStatus.SENT, notification.getStatus());
            assertNotNull(notification.getSentAt());
        });
        verify(realtimeEventPublisher).publish(firstRealtimeMessage);
        verify(realtimeEventPublisher).publish(secondRealtimeMessage);
        verify(realtimeEventPublisher, times(2)).publish(any(NotificationRealtimeMessage.class));
        assertEquals(List.of(savedFirstNotification, savedSecondNotification), result);
    }

    @Test
    void getMyNotifications_shouldMaterializeMissingActiveBroadcastAnnouncementsForCurrentRole() {
        UUID accountId = UUID.randomUUID();
        UUID broadcastId = UUID.randomUUID();
        CurrentAccountAccess currentAccount = new CurrentAccountAccess(
                accountId,
                "subject",
                "customer01",
                "customer@example.com",
                UUID.randomUUID(),
                "CUSTOMER",
                AccountStatus.ACTIVE,
                null,
                null,
                null,
                Set.of()
        );
        BroadcastAnnouncement announcement = new BroadcastAnnouncement();
        announcement.setBroadcastId(broadcastId);
        announcement.setNotificationType(NotificationType.SYSTEM_NOTICE);
        announcement.setTitle("Thông báo thay đổi giá");
        announcement.setMessage("Giá xe được thay đổi, vui lòng theo dõi.");
        announcement.setRedirectUrl("/pricing");

        Notification listedNotification = new Notification();
        listedNotification.setNotificationId(UUID.randomUUID());
        listedNotification.setAccountId(accountId);
        listedNotification.setBroadcastId(broadcastId);

        when(currentAccountPortIn.getCurrentAccountOrThrow()).thenReturn(currentAccount);
        when(broadcastAnnouncementPortOut.findActivePublishedForRole(eq("CUSTOMER"), any(Instant.class)))
                .thenReturn(List.of(announcement));
        when(notificationPortOut.findExistingBroadcastIdsForAccount(accountId, List.of(broadcastId)))
                .thenReturn(List.of());
        when(notificationPortOut.findByAccountId(eq(accountId), eq(false), any(Instant.class), eq(20)))
                .thenReturn(List.of(listedNotification));

        List<Notification> result = notificationUseCase.getMyNotifications(false, 20, null);

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationPortOut).saveAll(notificationsCaptor.capture());
        Notification materializedNotification = notificationsCaptor.getValue().get(0);
        assertEquals(accountId, materializedNotification.getAccountId());
        assertEquals(broadcastId, materializedNotification.getBroadcastId());
        assertEquals(NotificationType.SYSTEM_NOTICE, materializedNotification.getNotificationType());
        assertEquals("Thông báo thay đổi giá", materializedNotification.getTitle());
        assertEquals("Giá xe được thay đổi, vui lòng theo dõi.", materializedNotification.getMessage());
        assertEquals("/pricing", materializedNotification.getRedirectUrl());
        assertEquals(NotificationChannel.WEB, materializedNotification.getChannel());
        assertEquals(NotificationStatus.SENT, materializedNotification.getStatus());
        verify(realtimeEventPublisher, never()).publish(any(NotificationRealtimeMessage.class));
        assertEquals(List.of(listedNotification), result);
    }

    private Notification notification(UUID accountId, String title) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setNotificationType(NotificationType.PARKING_LOT_MAINTENANCE);
        notification.setTitle(title);
        notification.setMessage("Parking lot is under maintenance");
        notification.setRelatedSchema("parking");
        notification.setRelatedTable("parking_lots");
        notification.setRelatedId(UUID.randomUUID());
        return notification;
    }

    private NotificationRealtimeMessage realtimeMessage(Notification notification) {
        return new NotificationRealtimeMessage(
                notification.getNotificationId(),
                notification.getAccountId(),
                null,
                NotificationChannel.WEB,
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                NotificationStatus.SENT,
                "2026-07-30 10:00:00",
                null,
                null,
                notification.getRelatedSchema(),
                notification.getRelatedTable(),
                notification.getRelatedId(),
                "2026-07-30 10:00:00"
        );
    }
}
