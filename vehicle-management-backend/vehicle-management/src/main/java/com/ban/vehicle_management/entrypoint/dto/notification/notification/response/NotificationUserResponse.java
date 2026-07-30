package com.ban.vehicle_management.entrypoint.dto.notification.notification.response;

import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationUserResponse {

    private UUID notificationId;
    private NotificationChannel channel;
    private String title;
    private String message;
    private NotificationStatus status;
    private String sentAt;
    private String readAt;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private String createdAt;
}
