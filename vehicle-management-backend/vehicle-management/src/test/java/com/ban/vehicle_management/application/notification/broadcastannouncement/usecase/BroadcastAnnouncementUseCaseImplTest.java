package com.ban.vehicle_management.application.notification.broadcastannouncement.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.notification.broadcastannouncement.mapper.BroadcastAnnouncementDomainMapper;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BroadcastAnnouncementUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private BroadcastAnnouncementPortOut broadcastAnnouncementPortOut;

    @Mock
    private NotificationPortIn notificationPortIn;

    @Mock
    private BroadcastAnnouncementDomainMapper broadcastAnnouncementDomainMapper;

    @InjectMocks
    private BroadcastAnnouncementUseCaseImpl useCase;

    private BroadcastAnnouncement announcement;

    @BeforeEach
    void setUp() {
        announcement = validAnnouncement();
    }

    @Test
    void createBroadcastAnnouncement_shouldNormalizeAndPersistDraft() {
        announcement.setAudienceType(BroadcastAnnouncementAudienceType.ROLE_CODES);
        announcement.setRoleCodes(Set.of("customer"));
        when(broadcastAnnouncementPortOut.save(any(BroadcastAnnouncement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BroadcastAnnouncement result = useCase.createBroadcastAnnouncement(announcement);

        verify(currentAccountPortIn).requirePermission("BROADCAST_NOTIFICATION_CREATE_ALL");
        assertNotNull(result.getBroadcastId());
        assertEquals(BroadcastAnnouncementStatus.DRAFT, result.getStatus());
        assertEquals(Set.of("CUSTOMER"), result.getRoleCodes());
        assertEquals(Boolean.TRUE, result.getEnabled());
    }

    @Test
    void publishBroadcastAnnouncement_shouldFanOutAndMarkPublished() {
        UUID broadcastId = UUID.randomUUID();
        announcement.setBroadcastId(broadcastId);
        BroadcastNotificationCommand broadcastCommand = new BroadcastNotificationCommand(
                true,
                Set.of(),
                Set.of(),
                broadcastId,
                announcement.getNotificationType(),
                announcement.getTitle(),
                announcement.getMessage(),
                announcement.getRedirectUrl(),
                "notification",
                "broadcast_announcements",
                broadcastId
        );
        when(broadcastAnnouncementPortOut.findById(broadcastId)).thenReturn(Optional.of(announcement));
        when(broadcastAnnouncementDomainMapper.toBroadcastNotificationCommand(announcement, true))
                .thenReturn(broadcastCommand);
        when(notificationPortIn.sendBroadcastWebNotification(any(BroadcastNotificationCommand.class)))
                .thenReturn(List.of(new Notification()));
        when(broadcastAnnouncementPortOut.save(any(BroadcastAnnouncement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BroadcastAnnouncement result = useCase.publishBroadcastAnnouncement(broadcastId);

        ArgumentCaptor<BroadcastNotificationCommand> commandCaptor =
                ArgumentCaptor.forClass(BroadcastNotificationCommand.class);
        verify(notificationPortIn).sendBroadcastWebNotification(commandCaptor.capture());
        BroadcastNotificationCommand command = commandCaptor.getValue();

        verify(currentAccountPortIn).requirePermission("BROADCAST_NOTIFICATION_PUBLISH_ALL");
        assertEquals(broadcastId, command.broadcastId());
        assertEquals(announcement.getNotificationType(), command.notificationType());
        assertEquals(announcement.getRedirectUrl(), command.redirectUrl());
        assertEquals(BroadcastAnnouncementStatus.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedAt());
    }

    @Test
    void deleteBroadcastAnnouncement_shouldRejectPublishedAnnouncement() {
        UUID broadcastId = UUID.randomUUID();
        announcement.setBroadcastId(broadcastId);
        announcement.setStatus(BroadcastAnnouncementStatus.PUBLISHED);
        when(broadcastAnnouncementPortOut.findById(broadcastId)).thenReturn(Optional.of(announcement));

        assertThrows(
                ConflictException.class,
                () -> useCase.deleteBroadcastAnnouncement(broadcastId)
        );

        verify(currentAccountPortIn).requirePermission("BROADCAST_NOTIFICATION_DELETE_ALL");
    }

    private BroadcastAnnouncement validAnnouncement() {
        BroadcastAnnouncement value = new BroadcastAnnouncement();
        value.setNotificationType(NotificationType.SYSTEM_NOTICE);
        value.setTitle("Thong bao he thong");
        value.setMessage("Noi dung thong bao");
        value.setAudienceType(BroadcastAnnouncementAudienceType.ALL_ACTIVE_ACCOUNTS);
        value.setRoleCodes(Set.of());
        value.setStartAt(Instant.now());
        value.setEnabled(true);
        value.setRedirectUrl("/notifications");
        value.setStatus(BroadcastAnnouncementStatus.DRAFT);
        return value;
    }
}
