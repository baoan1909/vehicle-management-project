package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.RetrievalAuditResultEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetrievalAuditResultRepository extends JpaRepository<RetrievalAuditResultEntity, UUID> {

    List<RetrievalAuditResultEntity> findByRetrievalAuditIdOrderByFinalRankAsc(UUID retrievalAuditId);
}
