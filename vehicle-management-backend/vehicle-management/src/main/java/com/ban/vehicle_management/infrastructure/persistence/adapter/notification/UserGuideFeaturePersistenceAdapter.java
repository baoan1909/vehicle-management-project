package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideFeaturePortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.infrastructure.mapper.notification.UserGuidePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideFeatureRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.notification.UserGuideFeatureSpecifications;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class UserGuideFeaturePersistenceAdapter implements UserGuideFeaturePortOut {

    private final UserGuideFeatureRepository repository;
    private final UserGuidePersistenceMapper mapper;

    public UserGuideFeaturePersistenceAdapter(
            UserGuideFeatureRepository repository,
            UserGuidePersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserGuideFeature save(UserGuideFeature feature) {
        if (feature.getFeatureId() == null) {
            feature.setFeatureId(UUID.randomUUID());
        }
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(feature)));
    }

    @Override
    public Optional<UserGuideFeature> findById(UUID featureId) {
        return repository.findById(featureId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserGuideFeature> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public Page<UserGuideFeature> findPage(Boolean enabled, String keyword, Pageable pageable) {
        return repository
                .findAll(UserGuideFeatureSpecifications.withFilters(enabled, keyword), pageable)
                .map(mapper::toDomain);
    }
}
