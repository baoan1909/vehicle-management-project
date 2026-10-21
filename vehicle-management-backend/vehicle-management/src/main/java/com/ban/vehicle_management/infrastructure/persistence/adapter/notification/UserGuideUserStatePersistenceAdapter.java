package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideUserStatePortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideUserState;
import com.ban.vehicle_management.infrastructure.mapper.notification.UserGuidePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideUserStateRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserGuideUserStatePersistenceAdapter implements UserGuideUserStatePortOut {

    private final UserGuideUserStateRepository repository;
    private final UserGuidePersistenceMapper mapper;

    public UserGuideUserStatePersistenceAdapter(
            UserGuideUserStateRepository repository,
            UserGuidePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserGuideUserState save(UserGuideUserState state) {
        if (state.getUserGuideStateId() == null) {
            state.setUserGuideStateId(UUID.randomUUID());
        }
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(state)));
    }

    @Override
    public Optional<UserGuideUserState> findById(UUID stateId) {
        return repository.findById(stateId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserGuideUserState> findByAccountIdAndGuideId(UUID accountId, UUID guideId) {
        return repository.findByAccountIdAndGuideId(accountId, guideId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserGuideUserState> findByAccountIdAndGuideIdForUpdate(UUID accountId, UUID guideId) {
        return repository.findByAccountIdAndGuideIdForUpdate(accountId, guideId).map(mapper::toDomain);
    }
}
