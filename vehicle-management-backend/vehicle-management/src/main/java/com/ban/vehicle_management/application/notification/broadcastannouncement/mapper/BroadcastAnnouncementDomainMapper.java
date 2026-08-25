package com.ban.vehicle_management.application.notification.broadcastannouncement.mapper;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BroadcastAnnouncementDomainMapper {

    @Mapping(target = "broadcastId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEditableFields(
            BroadcastAnnouncement request,
            @MappingTarget BroadcastAnnouncement existing
    );

    @Mapping(target = "allActiveAccounts", source = "allActiveAccounts")
    @Mapping(target = "accountIds", ignore = true)
    @Mapping(target = "recipientCriteria", ignore = true)
    BroadcastNotificationCommand toBroadcastNotificationCommand(
            BroadcastAnnouncement announcement,
            boolean allActiveAccounts
    );
}
