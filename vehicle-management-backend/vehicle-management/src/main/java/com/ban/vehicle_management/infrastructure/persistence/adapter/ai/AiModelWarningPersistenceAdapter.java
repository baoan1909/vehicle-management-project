package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiModelWarningPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiModelWarningPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiModelWarningRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiModelWarningPersistenceAdapter implements AiModelWarningPortOut {

    private final AiModelWarningRepository repository;
    private final AiModelWarningPersistenceMapper mapper;

    public AiModelWarningPersistenceAdapter(AiModelWarningRepository repository, AiModelWarningPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AiModelWarning save(AiModelWarning warning) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(warning)));
    }

    @Override
    public List<AiModelWarning> findByStatuses(Collection<AiModelWarningStatus> statuses) {
        return repository.findByStatusInOrderByDetectedAtDesc(statuses).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<AiModelWarning> findOpen(String provider, String modelId, UUID configurationId, String warningCode) {
        return repository.findFirstByProviderAndModelIdAndConfigurationIdAndWarningCodeAndStatusIn(
                        AiProvider.valueOf(provider),
                        modelId,
                        configurationId,
                        warningCode,
                        List.of(AiModelWarningStatus.OPEN, AiModelWarningStatus.ACKNOWLEDGED)
                )
                .map(mapper::toDomain);
    }
}
