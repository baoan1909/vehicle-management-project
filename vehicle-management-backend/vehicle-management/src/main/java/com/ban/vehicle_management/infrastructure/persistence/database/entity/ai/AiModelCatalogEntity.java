package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_model_catalog", schema = "ai")
@IdClass(AiModelCatalogEntity.Key.class)
@Getter
@Setter
public class AiModelCatalogEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AiProvider provider;

    @Id
    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "input_token_limit")
    private Integer inputTokenLimit;

    @Column(name = "output_token_limit")
    private Integer outputTokenLimit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "supported_actions", columnDefinition = "jsonb", nullable = false)
    private String supportedActions;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AiModelStatus status;

    @Getter
    @Setter
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private AiProvider provider;
        private String modelId;
    }
}
