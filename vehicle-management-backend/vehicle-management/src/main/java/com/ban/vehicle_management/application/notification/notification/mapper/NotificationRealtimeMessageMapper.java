package com.ban.vehicle_management.application.notification.notification.mapper;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationRealtimeMessageMapper {

    NotificationRealtimeMessage toRealtimeMessage(Notification notification);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
