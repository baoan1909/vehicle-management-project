package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuidePortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.infrastructure.mapper.notification.UserGuidePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.notification.UserGuideSpecifications;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideDisplayType;
import com.ban.vehicle_management.shared.enumeration.notification.UserGuideStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class UserGuidePersistenceAdapter implements UserGuidePortOut {

    private final UserGuideRepository repository;
    private final UserGuidePersistenceMapper mapper;

    public UserGuidePersistenceAdapter(UserGuideRepository repository, UserGuidePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserGuide save(UserGuide guide) {
        if (guide.getGuideId() == null) {
            guide.setGuideId(UUID.randomUUID());
        }
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(guide)));
    }

    @Override
    public Optional<UserGuide> findById(UUID guideId) {
        return repository.findById(guideId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserGuide> findByIdForUpdate(UUID guideId) {
        return repository.findByIdForUpdate(guideId).map(mapper::toDomain);
    }

    @Override
    public Optional<UserGuide> findByCode(String code) {
        return repository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }

    @Override
    public List<UserGuide> findByFeatureIdAndStatus(UUID featureId, UserGuideStatus status) {
        return repository.findByFeatureIdAndStatus(featureId, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<UserGuide> findPage(
            UUID featureId,
            UserGuideStatus status,
            UserGuideDisplayType displayType,
            String keyword,
            Pageable pageable
    ) {
        return repository
                .findAll(UserGuideSpecifications.withFilters(featureId, status, displayType, keyword), pageable)
                .map(mapper::toDomain);
    }
}
