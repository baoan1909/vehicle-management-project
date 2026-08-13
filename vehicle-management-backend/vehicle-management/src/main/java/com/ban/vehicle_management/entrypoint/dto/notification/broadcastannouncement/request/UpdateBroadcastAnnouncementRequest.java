package com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request;

import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UpdateBroadcastAnnouncementRequest(
        NotificationType notificationType,
        String title,
        String message,
        BroadcastAnnouncementAudienceType audienceType,
        Set<String> roleCodes,
        Instant startAt,
        Instant endAt,
        Integer displayOrder,
        Boolean enabled,
        String redirectUrl,
        String relatedSchema,
        String relatedTable,
        UUID relatedId
) {
}
