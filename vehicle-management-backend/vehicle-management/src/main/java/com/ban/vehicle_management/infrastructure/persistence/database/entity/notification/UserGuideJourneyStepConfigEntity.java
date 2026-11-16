package com.ban.vehicle_management.infrastructure.persistence.database.entity.notification;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_guide_journey_step_configs", schema = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGuideJourneyStepConfigEntity {
    @Id
    @Column(name = "step_id", nullable = false)
    private UUID stepId;
    @Column(name = "guide_version_id", nullable = false)
    private UUID guideVersionId;
    @Column(name = "step_key", nullable = false, length = 120)
    private String stepKey;
    @Column(name = "readiness_key", nullable = false, length = 120)
    private String readinessKey;
    @Column(name = "route_template", length = 1000)
    private String routeTemplate;
    @Column(name = "action_label", length = 100)
    private String actionLabel;
    @Column(name = "linked_guide_id")
    private UUID linkedGuideId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_content", nullable = false, columnDefinition = "jsonb")
    private JsonNode stateContent;
}
