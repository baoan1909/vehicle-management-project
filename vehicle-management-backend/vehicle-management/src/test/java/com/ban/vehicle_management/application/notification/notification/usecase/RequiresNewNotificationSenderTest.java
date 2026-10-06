package com.ban.vehicle_management.application.notification.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

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
    void shouldAlwaysStartIndependentTransaction() throws NoSuchMethodException {
        Method method = RequiresNewNotificationSender.class.getMethod("send", SendNotificationCommand.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation());
    }
}
