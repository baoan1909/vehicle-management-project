package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideAudienceMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_guide_versions", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideVersionEntity extends AuditableEntity {

    @Id
    @Column(name = "guide_version_id", nullable = false)
    private UUID guideVersionId;

    @Column(name = "guide_id", nullable = false)
    private UUID guideId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "summary")
    private String summary;

    @Column(name = "deep_link", length = 1000)
    private String deepLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_mode", nullable = false, length = 30)
    private UserGuideAudienceMode audienceMode;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;
}
