package com.ban.vehicle_management.infrastructure.mapper.notification;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.BroadcastAnnouncementEntity;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BroadcastAnnouncementPersistenceMapper {

    BroadcastAnnouncementEntity toEntity(BroadcastAnnouncement domain);

    BroadcastAnnouncement toDomain(BroadcastAnnouncementEntity entity);

    List<BroadcastAnnouncement> toDomains(List<BroadcastAnnouncementEntity> entities);
}
