package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeDocumentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeDocumentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentCommandRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeDocumentRepository;
import com.ban.vehicle_management.infrastructure.persistence.specification.ai.KnowledgeDocumentSpecifications;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KnowledgeDocumentPersistenceAdapter implements KnowledgeDocumentPortOut {

    private final KnowledgeDocumentRepository repository;
    private final KnowledgeDocumentCommandRepository commandRepository;
    private final KnowledgeDocumentPersistenceMapper mapper;

    public KnowledgeDocumentPersistenceAdapter(
            KnowledgeDocumentRepository repository,
            KnowledgeDocumentCommandRepository commandRepository,
            KnowledgeDocumentPersistenceMapper mapper) {
        this.repository = repository;
        this.commandRepository = commandRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public KnowledgeDocument save(KnowledgeDocument document) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(document)));
    }

    @Override
    @Transactional
    public KnowledgeDocument insertIdempotent(KnowledgeDocument document) {
        KnowledgeDocumentEntity entity = mapper.toEntity(document);
        Optional<UUID> insertedId = commandRepository.insertIfAbsent(entity);
        if (insertedId.isEmpty()) {
            // Conflict on (source_id, checksum_sha256, document_version)
            // Find the existing document by logical identity: source_id + document_version + checksum
            return repository.findBySourceIdAndChecksumSha256AndDocumentVersion(
                            document.getSourceId(), document.getChecksumSha256(), document.getDocumentVersion())
                    .map(mapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException(
                            "Insert conflict but no existing document found for source=" + document.getSourceId()
                                    + ", checksum=" + document.getChecksumSha256()
                                    + ", version=" + document.getDocumentVersion()));
        }
        return repository.findById(insertedId.get()).map(mapper::toDomain).orElse(document);
    }

    @Override
    public Optional<KnowledgeDocument> findById(UUID documentId) {
        return repository.findById(documentId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<KnowledgeDocument> findByIdForUpdate(UUID documentId) {
        return repository.findByIdForUpdate(documentId).map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeDocument> findLatestByDocumentKey(UUID sourceId, UUID documentKey) {
        return repository.findFirstBySourceIdAndDocumentKeyOrderByDocumentVersionDesc(sourceId, documentKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<KnowledgeDocument> findBySourceIdAndChecksum(UUID sourceId, String checksumSha256) {
        return repository.findFirstBySourceIdAndChecksumSha256OrderByCreatedAtDesc(sourceId, checksumSha256)
                .map(mapper::toDomain);
    }

    @Override
    public List<KnowledgeDocument> findBySourceId(UUID sourceId) {
        return repository.findBySourceIdOrderByCreatedAtDesc(sourceId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<KnowledgeDocument> findAll(KnowledgeDocumentQuery query, Pageable pageable) {
        KnowledgeDocumentSpecifications.Filter filter = new KnowledgeDocumentSpecifications.Filter(
                query.sourceId(), query.documentKey(), query.status(), query.accessScope(),
                query.tenantId(), query.fileExtension(), query.createdFrom(), query.createdTo(), query.keyword());
        return repository.findAll(filter.toSpecification(), pageable).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<LockedDocumentVersion> lockLatestVersionByDocumentKey(UUID documentKey) {
        commandRepository.lockDocumentKey(documentKey);
        return repository.findFirstByDocumentKeyOrderByDocumentVersionDesc(documentKey)
                .map(entity -> new LockedDocumentVersion(
                        entity.getDocumentId(),
                        entity.getDocumentVersion(),
                        entity.getStatus()));
    }
}
