package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiMessageCitationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiMessageCitationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiMessageCitationEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiMessageCitationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AiMessageCitationPersistenceAdapter implements AiMessageCitationPortOut {

    private final AiMessageCitationRepository repository;
    private final AiMessageCitationPersistenceMapper mapper;

    public AiMessageCitationPersistenceAdapter(
            AiMessageCitationRepository repository,
            AiMessageCitationPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public List<AiMessageCitation> saveAll(UUID messageId, List<AiMessageCitation> citations) {
        Instant now = Instant.now();
        List<AiMessageCitationEntity> entities = citations.stream()
                .map(citation -> {
                    AiMessageCitationEntity entity = mapper.toEntity(citation);
                    if (entity.getCitationId() == null) {
                        entity.setCitationId(UUID.randomUUID());
                    }
                    entity.setMessageId(messageId);
                    entity.setCreatedAt(now);
                    return entity;
                })
                .toList();
        return repository.saveAllAndFlush(entities).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AiMessageCitation> findByMessageId(UUID messageId) {
        return repository.findByMessageIdOrderByCitationOrderAsc(messageId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
