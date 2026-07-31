package com.ban.vehicle_management.domain.notification.broadcastannouncement.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastAnnouncement extends AuditableDomainModel {

    private UUID broadcastId;
    private NotificationType notificationType;
    private String title;
    private String message;
    private BroadcastAnnouncementAudienceType audienceType;
    private Set<String> roleCodes;
    private Instant startAt;
    private Instant endAt;
    private Boolean enabled;
    private String redirectUrl;
    private BroadcastAnnouncementStatus status;
    private Instant publishedAt;
    private Instant cancelledAt;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
}
