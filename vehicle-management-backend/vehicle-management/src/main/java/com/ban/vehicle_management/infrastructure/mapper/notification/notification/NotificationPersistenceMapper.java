package com.ban.vehicle_management.infrastructure.mapper.notification.notification;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.infrastructure.persistence.notification.notification.NotificationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPersistenceMapper {

    NotificationEntity toEntity(Notification domain);

    Notification toDomain(NotificationEntity entity);
}
