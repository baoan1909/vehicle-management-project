package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knowledge_staged_embeddings", schema = "ai")
@Getter
@NoArgsConstructor
public class KnowledgeStagedEmbeddingEntity {
    @Id
    @Column(name = "staged_embedding_id", nullable = false)
    private UUID stagedEmbeddingId;
}
