package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiModelConfigurationPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiModelConfigurationRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiModelConfigurationPersistenceAdapter implements AiModelConfigurationPortOut {

    private final AiModelConfigurationRepository repository;
    private final AiModelConfigurationPersistenceMapper mapper;

    public AiModelConfigurationPersistenceAdapter(
            AiModelConfigurationRepository repository,
            AiModelConfigurationPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<AiModelConfiguration> findEnabledByUseCase(AiUseCase useCase) {
        return repository.findByUseCaseAndStatusInOrderByPriorityAscCreatedAtAsc(
                        useCase,
                        List.of(AiModelStatus.ACTIVE, AiModelStatus.FALLBACK)
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AiModelConfiguration> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AiModelConfiguration> findById(UUID configurationId) {
        return repository.findById(configurationId).map(mapper::toDomain);
    }

    @Override
    public AiModelConfiguration save(AiModelConfiguration configuration) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(configuration)));
    }

    @Override
    public List<AiModelConfiguration> findByUseCaseAndStatus(AiUseCase useCase, AiModelStatus status) {
        return repository.findByUseCaseAndStatusOrderByPriorityAscCreatedAtAsc(useCase, status).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
