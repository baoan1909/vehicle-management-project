package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.infrastructure.mapper.ai.RetrievalAuditPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.RetrievalAuditEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.RetrievalAuditResultEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.RetrievalAuditRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.RetrievalAuditResultRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RetrievalAuditPersistenceAdapter implements KnowledgeRetrievalAuditPortOut {

    private final RetrievalAuditRepository auditRepository;
    private final RetrievalAuditResultRepository resultRepository;
    private final RetrievalAuditPersistenceMapper mapper;

    public RetrievalAuditPersistenceAdapter(
            RetrievalAuditRepository auditRepository,
            RetrievalAuditResultRepository resultRepository,
            RetrievalAuditPersistenceMapper mapper) {
        this.auditRepository = auditRepository;
        this.resultRepository = resultRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public RetrievalAudit save(RetrievalAudit audit) {
        RetrievalAuditEntity entity = mapper.toEntity(audit);
        if (entity.getRetrievalAuditId() == null) {
            entity.setRetrievalAuditId(UUID.randomUUID());
        }
        if (entity.getResultCount() == null) {
            entity.setResultCount(entity.getFinalResultCount() == null ? 0 : entity.getFinalResultCount());
        }
        RetrievalAuditEntity saved = auditRepository.saveAndFlush(entity);
        List<RetrievalAuditResultEntity> results = audit.results() == null
                ? List.of()
                : audit.results().stream()
                        .map(result -> {
                            RetrievalAuditResultEntity resultEntity = mapper.toResultEntity(result);
                            if (resultEntity.getAuditResultId() == null) {
                                resultEntity.setAuditResultId(UUID.randomUUID());
                            }
                            resultEntity.setRetrievalAuditId(saved.getRetrievalAuditId());
                            resultEntity.setCreatedAt(saved.getCreatedAt());
                            return resultEntity;
                        })
                        .toList();
        List<RetrievalAuditResultEntity> savedResults = resultRepository.saveAllAndFlush(results);
        return mapper.toDomain(
                saved,
                savedResults.stream().map(mapper::toResultDomain).toList());
    }

    @Override
    public Optional<RetrievalAudit> findById(UUID retrievalAuditId) {
        return auditRepository.findById(retrievalAuditId)
                .map(entity -> mapper.toDomain(
                        entity,
                        resultRepository.findByRetrievalAuditIdOrderByFinalRankAsc(entity.getRetrievalAuditId())
                                .stream()
                                .map(mapper::toResultDomain)
                                .toList()));
    }

    @Override
    @Transactional
    public void finalizeForAnswer(
            UUID retrievalAuditId,
            UUID runId,
            UUID outputMessageId,
            Set<UUID> selectedChunkIds) {
        RetrievalAuditEntity audit = auditRepository.findById(retrievalAuditId)
                .orElseThrow(() -> new IllegalStateException("Retrieval audit does not exist"));
        audit.setRunId(runId);
        audit.setOutputMessageId(outputMessageId);
        auditRepository.save(audit);

        Set<UUID> selected = selectedChunkIds == null ? Set.of() : Set.copyOf(selectedChunkIds);
        List<RetrievalAuditResultEntity> results =
                resultRepository.findByRetrievalAuditIdOrderByFinalRankAsc(retrievalAuditId);
        results.forEach(result -> result.setSelectedForContext(selected.contains(result.getChunkId())));
        resultRepository.saveAllAndFlush(results);
    }
}
