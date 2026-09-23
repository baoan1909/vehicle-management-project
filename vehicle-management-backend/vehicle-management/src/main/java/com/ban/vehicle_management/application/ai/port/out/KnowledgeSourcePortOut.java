package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.application.ai.query.KnowledgeSourceQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeSourcePortOut {

    KnowledgeSource save(KnowledgeSource source);

    Optional<KnowledgeSource> findById(UUID sourceId);

    Optional<KnowledgeSource> findByIdForUpdate(UUID sourceId);

    List<KnowledgeSource> findAll();

    Page<KnowledgeSource> findAll(KnowledgeSourceQuery query, Pageable pageable);

    boolean existsByTitle(String title);
}
