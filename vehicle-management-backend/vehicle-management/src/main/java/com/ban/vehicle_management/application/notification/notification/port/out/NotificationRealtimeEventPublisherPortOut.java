package com.ban.vehicle_management.application.notification.notification.port.out;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;

public interface NotificationRealtimeEventPublisherPortOut {

    void publish(NotificationRealtimeMessage message);
}
