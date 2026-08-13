package com.ban.vehicle_management.application.notification.notification.mapper;

import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationCommandMapper {

    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "realtimeDeliveredAt", ignore = true)
    @Mapping(target = "broadcastId", ignore = true)
    @Mapping(target = "redirectUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Notification toDomain(SendNotificationCommand command);

    @Mapping(target = "accountId", source = "accountId")
    @Mapping(target = "notificationId", ignore = true)
    @Mapping(target = "channel", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "realtimeDeliveredAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Notification toDomain(BroadcastNotificationCommand command, UUID accountId);
}
