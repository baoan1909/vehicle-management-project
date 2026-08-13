package com.ban.vehicle_management.infrastructure.mapper.notification;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.NotificationEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationPersistenceMapper {

    NotificationEntity toEntity(Notification domain);

    List<NotificationEntity> toEntities(List<Notification> domains);

    Notification toDomain(NotificationEntity entity);

    List<Notification> toDomains(List<NotificationEntity> entities);
}


