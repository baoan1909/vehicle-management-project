package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.userguide.port.out.UserGuideVersionPortOut;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideAudience;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideStep;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.infrastructure.mapper.notification.UserGuidePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideAudienceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideStepEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.UserGuideVersionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideAudienceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideStepRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.UserGuideVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserGuideVersionPersistenceAdapter implements UserGuideVersionPortOut {

    private final UserGuideVersionRepository versionRepository;
    private final UserGuideStepRepository stepRepository;
    private final UserGuideAudienceRepository audienceRepository;
    private final UserGuidePersistenceMapper mapper;

    public UserGuideVersionPersistenceAdapter(
            UserGuideVersionRepository versionRepository,
            UserGuideStepRepository stepRepository,
            UserGuideAudienceRepository audienceRepository,
            UserGuidePersistenceMapper mapper
    ) {
        this.versionRepository = versionRepository;
        this.stepRepository = stepRepository;
        this.audienceRepository = audienceRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserGuideVersion save(UserGuideVersion version) {
        if (version.getGuideVersionId() == null) {
            version.setGuideVersionId(UUID.randomUUID());
        }
        prepareChildren(version);
        UserGuideVersionEntity savedVersion = versionRepository.saveAndFlush(mapper.toEntity(version));

        stepRepository.deleteAllByGuideVersionId(savedVersion.getGuideVersionId());
        audienceRepository.deleteAllByGuideVersionId(savedVersion.getGuideVersionId());
        stepRepository.flush();
        audienceRepository.flush();

        List<UserGuideStepEntity> stepEntities = mapper.toStepEntities(version.getSteps());
        List<UserGuideAudienceEntity> audienceEntities = mapper.toAudienceEntities(version.getAudiences());
        stepRepository.saveAll(stepEntities);
        audienceRepository.saveAll(audienceEntities);
        stepRepository.flush();
        audienceRepository.flush();
        return loadAggregate(savedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserGuideVersion> findById(UUID guideVersionId) {
        return versionRepository.findById(guideVersionId).map(this::loadAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserGuideVersion> findByGuideIdAndVersionNumber(UUID guideId, Integer versionNumber) {
        return versionRepository.findByGuideIdAndVersionNumber(guideId, versionNumber).map(this::loadAggregate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserGuideVersion> findByGuideIdOrderByVersionNumberDesc(UUID guideId) {
        return versionRepository.findByGuideIdOrderByVersionNumberDesc(guideId).stream()
                .map(this::loadAggregate)
                .toList();
    }

    private void prepareChildren(UserGuideVersion version) {
        UUID versionId = version.getGuideVersionId();
        for (UserGuideStep step : version.getSteps()) {
            if (step.getStepId() == null) {
                step.setStepId(UUID.randomUUID());
            }
            step.setGuideVersionId(versionId);
        }
        for (UserGuideAudience audience : version.getAudiences()) {
            if (audience.getAudienceId() == null) {
                audience.setAudienceId(UUID.randomUUID());
            }
            audience.setGuideVersionId(versionId);
            if (audience.getCreatedAt() == null) {
                audience.setCreatedAt(Instant.now());
            }
        }
    }

    private UserGuideVersion loadAggregate(UserGuideVersionEntity entity) {
        UserGuideVersion domain = mapper.toDomain(entity);
        domain.setSteps(mapper.toStepDomains(
                stepRepository.findByGuideVersionIdOrderByDisplayOrderAsc(entity.getGuideVersionId())));
        domain.setAudiences(mapper.toAudienceDomains(
                audienceRepository.findByGuideVersionIdOrderByCreatedAtAscAudienceIdAsc(
                        entity.getGuideVersionId())));
        return domain;
    }
}
