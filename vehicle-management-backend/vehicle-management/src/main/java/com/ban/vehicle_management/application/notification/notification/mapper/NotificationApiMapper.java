package com.ban.vehicle_management.application.notification.notification.mapper;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUserResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationApiMapper {

    NotificationUserResponse toUserResponse(Notification notification);

    List<NotificationUserResponse> toUserResponses(List<Notification> notifications);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
