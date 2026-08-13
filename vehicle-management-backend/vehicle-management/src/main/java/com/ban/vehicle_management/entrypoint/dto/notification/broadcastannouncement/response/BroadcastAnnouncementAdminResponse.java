package com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.response;

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
    private String startAt;
    private String endAt;
    private Integer displayOrder;
    private Boolean enabled;
    private String redirectUrl;
    private BroadcastAnnouncementStatus status;
    private String publishedAt;
    private String cancelledAt;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}
