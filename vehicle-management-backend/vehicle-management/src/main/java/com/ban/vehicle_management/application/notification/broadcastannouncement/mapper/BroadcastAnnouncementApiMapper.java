package com.ban.vehicle_management.application.notification.broadcastannouncement.mapper;

import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.CreateBroadcastAnnouncementRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.UpdateBroadcastAnnouncementRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.response.BroadcastAnnouncementAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BroadcastAnnouncementApiMapper {

    @Mapping(target = "broadcastId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    BroadcastAnnouncement toDomain(CreateBroadcastAnnouncementRequest request);

    @Mapping(target = "broadcastId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    BroadcastAnnouncement toDomain(UpdateBroadcastAnnouncementRequest request);

    BroadcastAnnouncementAdminResponse toAdminResponse(BroadcastAnnouncement announcement);

    List<BroadcastAnnouncementAdminResponse> toAdminResponses(List<BroadcastAnnouncement> announcements);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}
