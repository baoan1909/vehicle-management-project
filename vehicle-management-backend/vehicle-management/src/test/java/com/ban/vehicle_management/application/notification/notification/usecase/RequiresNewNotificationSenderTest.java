package com.ban.vehicle_management.application.notification.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class RequiresNewNotificationSenderTest {

    @Test
    void shouldDelegateWebNotification() {
        NotificationPortIn notificationPortIn = org.mockito.Mockito.mock(NotificationPortIn.class);
        RequiresNewNotificationSender sender = new RequiresNewNotificationSender(notificationPortIn);
        SendNotificationCommand command = new SendNotificationCommand(
                UUID.randomUUID(),
                NotificationType.AVATAR_APPROVED,
                "Ảnh đại diện đã được duyệt",
                "Ảnh đại diện mới của bạn đang được hiển thị.",
                "/",
                "people",
                "user_profile_avatars",
                UUID.randomUUID()
        );

        sender.send(command);

        verify(notificationPortIn).sendWebNotification(command);
    }

    @Test
    void shouldDelegateBroadcastWebNotification() {
        NotificationPortIn notificationPortIn = org.mockito.Mockito.mock(NotificationPortIn.class);
        RequiresNewNotificationSender sender = new RequiresNewNotificationSender(notificationPortIn);
        BroadcastNotificationCommand command = new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                NotificationType.ACCOUNT_PROFILE_SUBMITTED,
                "Có hồ sơ cần duyệt",
                "Có yêu cầu phê duyệt mới cần xử lý.",
                "/admin/partner-registrations",
                "operations",
                "approval_requests",
                UUID.randomUUID()
        );

        sender.sendBroadcast(command);

        verify(notificationPortIn).sendBroadcastWebNotification(command);
    }

    @Test
    void shouldAlwaysStartIndependentTransaction() throws NoSuchMethodException {
        Method sendMethod = RequiresNewNotificationSender.class.getMethod("send", SendNotificationCommand.class);
        Method broadcastMethod = RequiresNewNotificationSender.class.getMethod(
                "sendBroadcast",
                BroadcastNotificationCommand.class
        );
        Transactional sendTransactional = sendMethod.getAnnotation(Transactional.class);
        Transactional broadcastTransactional = broadcastMethod.getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, sendTransactional.propagation());
        assertEquals(Propagation.REQUIRES_NEW, broadcastTransactional.propagation());
    }
}
