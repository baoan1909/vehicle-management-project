package com.ban.vehicle_management.application.notification.notification.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a notification in an independent transaction.
 *
 * <p>This sender is intended for callbacks that run after a business transaction commits. Using a
 * new transaction is required because resources from the completed transaction can still be bound
 * while Spring invokes {@code afterCommit}; joining that transaction would not result in another
 * commit and the notification/realtime event could be lost.</p>
 */
@Service
public class RequiresNewNotificationSender {

    private final NotificationPortIn notificationPortIn;

    public RequiresNewNotificationSender(NotificationPortIn notificationPortIn) {
        this.notificationPortIn = notificationPortIn;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(SendNotificationCommand command) {
        notificationPortIn.sendWebNotification(command);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendBroadcast(BroadcastNotificationCommand command) {
        notificationPortIn.sendBroadcastWebNotification(command);
    }
}
