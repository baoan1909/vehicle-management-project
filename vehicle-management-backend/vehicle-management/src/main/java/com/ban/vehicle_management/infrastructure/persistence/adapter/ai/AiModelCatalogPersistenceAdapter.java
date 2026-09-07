package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiModelCatalogPortOut;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelCatalogEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiModelCatalogRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiModelCatalogPersistenceAdapter implements AiModelCatalogPortOut {

    private final AiModelCatalogRepository repository;
    private final ObjectMapper objectMapper;

    public AiModelCatalogPersistenceAdapter(AiModelCatalogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void upsertGeminiModels(List<AiProviderModel> models) {
        Instant now = Instant.now();
        for (AiProviderModel model : models) {
            AiModelCatalogEntity.Key key = new AiModelCatalogEntity.Key();
            key.setProvider(model.provider());
            key.setModelId(model.modelId());
            AiModelCatalogEntity entity = repository.findById(key).orElseGet(() -> {
                AiModelCatalogEntity created = new AiModelCatalogEntity();
                created.setProvider(model.provider());
                created.setModelId(model.modelId());
                created.setDiscoveredAt(now);
                created.setStatus(AiModelStatus.CANDIDATE);
                return created;
            });
            entity.setDisplayName(model.displayName());
            entity.setModelVersion(model.modelVersion());
            entity.setInputTokenLimit(model.inputTokenLimit());
            entity.setOutputTokenLimit(model.outputTokenLimit());
            entity.setSupportedActions(toJson(model.supportedActions()));
            entity.setLastSeenAt(now);
            repository.save(entity);
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
