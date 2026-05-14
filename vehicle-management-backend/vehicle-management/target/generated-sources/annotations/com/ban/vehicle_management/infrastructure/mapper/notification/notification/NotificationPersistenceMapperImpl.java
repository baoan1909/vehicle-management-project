package com.ban.vehicle_management.infrastructure.mapper.notification.notification;

import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.infrastructure.persistence.notification.notification.NotificationEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class NotificationPersistenceMapperImpl implements NotificationPersistenceMapper {

    @Override
    public NotificationEntity toEntity(Notification domain) {
        if ( domain == null ) {
            return null;
        }

        NotificationEntity notificationEntity = new NotificationEntity();

        notificationEntity.setCreatedAt( domain.getCreatedAt() );
        notificationEntity.setCreatedBy( domain.getCreatedBy() );
        notificationEntity.setUpdatedAt( domain.getUpdatedAt() );
        notificationEntity.setUpdatedBy( domain.getUpdatedBy() );
        notificationEntity.setNotificationId( domain.getNotificationId() );
        notificationEntity.setAccountId( domain.getAccountId() );
        notificationEntity.setChannel( domain.getChannel() );
        notificationEntity.setTitle( domain.getTitle() );
        notificationEntity.setMessage( domain.getMessage() );
        notificationEntity.setStatus( domain.getStatus() );
        notificationEntity.setSentAt( domain.getSentAt() );
        notificationEntity.setReadAt( domain.getReadAt() );
        notificationEntity.setRelatedSchema( domain.getRelatedSchema() );
        notificationEntity.setRelatedTable( domain.getRelatedTable() );
        notificationEntity.setRelatedId( domain.getRelatedId() );

        return notificationEntity;
    }

    @Override
    public Notification toDomain(NotificationEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Notification notification = new Notification();

        notification.setCreatedAt( entity.getCreatedAt() );
        notification.setCreatedBy( entity.getCreatedBy() );
        notification.setUpdatedAt( entity.getUpdatedAt() );
        notification.setUpdatedBy( entity.getUpdatedBy() );
        notification.setNotificationId( entity.getNotificationId() );
        notification.setAccountId( entity.getAccountId() );
        notification.setChannel( entity.getChannel() );
        notification.setTitle( entity.getTitle() );
        notification.setMessage( entity.getMessage() );
        notification.setStatus( entity.getStatus() );
        notification.setSentAt( entity.getSentAt() );
        notification.setReadAt( entity.getReadAt() );
        notification.setRelatedSchema( entity.getRelatedSchema() );
        notification.setRelatedTable( entity.getRelatedTable() );
        notification.setRelatedId( entity.getRelatedId() );

        return notification;
    }
}
