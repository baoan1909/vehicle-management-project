package com.ban.vehicle_management.application.notification.notification.mapper;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationRealtimeMessageMapper {

    default NotificationRealtimeMessage toRealtimeMessage(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationRealtimeMessage(
                notification.getNotificationId(),
                notification.getAccountId(),
                notification.getChannel(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                map(notification.getSentAt()),
                map(notification.getReadAt()),
                notification.getRelatedSchema(),
                notification.getRelatedTable(),
                notification.getRelatedId(),
                map(notification.getCreatedAt())
        );
    }

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
