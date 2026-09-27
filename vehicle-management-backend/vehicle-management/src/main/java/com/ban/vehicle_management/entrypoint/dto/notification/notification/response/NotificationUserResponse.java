package com.ban.vehicle_management.entrypoint.dto.notification.notification.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationUserResponse {

    private UUID notificationId;
    private UUID broadcastId;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private String title;
    private String message;
    private NotificationStatus status;
    private Instant sentAt;
    private Instant readAt;
    private String redirectUrl;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private Instant createdAt;
}
