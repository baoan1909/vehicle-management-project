package com.ban.vehicle_management.domain.notification.notification.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends AuditableDomainModel {

    private UUID notificationId;
    private UUID accountId;
    private UUID broadcastId;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private String title;
    private String message;
    private NotificationStatus status;
    private Instant sentAt;
    private Instant readAt;
    private Instant realtimeDeliveredAt;
    private String redirectUrl;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
}

