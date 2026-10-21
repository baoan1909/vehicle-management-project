package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStepPlacement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_guide_steps", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideStepEntity {

    @Id
    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "guide_version_id", nullable = false)
    private UUID guideVersionId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false)
    private String body;

    @Column(name = "media_object_key", length = 500)
    private String mediaObjectKey;

    @Column(name = "anchor_key", length = 120)
    private String anchorKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "placement", length = 30)
    private UserGuideStepPlacement placement;

    @Column(name = "collapse_ms", nullable = false)
    private Integer collapseMs;
}
