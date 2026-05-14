package com.ban.vehicle_management.infrastructure.persistence.notification.notification;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notifications", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity extends AuditableEntity {

    @Id
    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "related_schema")
    private String relatedSchema;

    @Column(name = "related_table")
    private String relatedTable;

    @Column(name = "related_id")
    private UUID relatedId;

}
