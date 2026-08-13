package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "broadcast_announcements", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastAnnouncementEntity extends AuditableEntity {

    @Id
    @Column(name = "broadcast_id", nullable = false)
    private UUID broadcastId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false)
    private BroadcastAnnouncementAudienceType audienceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "role_codes", columnDefinition = "jsonb")
    private Set<String> roleCodes;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BroadcastAnnouncementStatus status;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "related_schema")
    private String relatedSchema;

    @Column(name = "related_table")
    private String relatedTable;

    @Column(name = "related_id")
    private UUID relatedId;
}
