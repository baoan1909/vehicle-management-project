package com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BroadcastAnnouncementAdminResponse {

    private UUID broadcastId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private BroadcastAnnouncementAudienceType audienceType;
    private Set<String> roleCodes;
    private Instant startAt;
    private Instant endAt;
    private Integer displayOrder;
    private Boolean enabled;
    private String redirectUrl;
    private BroadcastAnnouncementStatus status;
    private Instant publishedAt;
    private Instant cancelledAt;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
