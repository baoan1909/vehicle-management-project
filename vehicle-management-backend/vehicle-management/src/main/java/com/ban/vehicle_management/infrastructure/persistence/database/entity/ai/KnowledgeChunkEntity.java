package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knowledge_chunks", schema = "ai")
@Getter
@NoArgsConstructor
public class KnowledgeChunkEntity {
    @Id
    @Column(name = "chunk_id", nullable = false)
    private UUID chunkId;
}
