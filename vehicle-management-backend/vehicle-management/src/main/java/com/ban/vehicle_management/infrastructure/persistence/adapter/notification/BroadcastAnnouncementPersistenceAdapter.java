package com.ban.vehicle_management.infrastructure.persistence.adapter.notification;

import com.ban.vehicle_management.application.notification.broadcastannouncement.port.out.BroadcastAnnouncementPortOut;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.infrastructure.mapper.notification.BroadcastAnnouncementPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.notification.BroadcastAnnouncementEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.notification.BroadcastAnnouncementRepository;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementAudienceType;
import com.ban.vehicle_management.shared.enumeration.notification.BroadcastAnnouncementStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BroadcastAnnouncementPersistenceAdapter implements BroadcastAnnouncementPortOut {

    private final BroadcastAnnouncementRepository broadcastAnnouncementRepository;
    private final BroadcastAnnouncementPersistenceMapper broadcastAnnouncementPersistenceMapper;

    public BroadcastAnnouncementPersistenceAdapter(
            BroadcastAnnouncementRepository broadcastAnnouncementRepository,
            BroadcastAnnouncementPersistenceMapper broadcastAnnouncementPersistenceMapper
    ) {
        this.broadcastAnnouncementRepository = broadcastAnnouncementRepository;
        this.broadcastAnnouncementPersistenceMapper = broadcastAnnouncementPersistenceMapper;
    }

    @Override
    public BroadcastAnnouncement save(BroadcastAnnouncement announcement) {
        BroadcastAnnouncementEntity savedEntity = broadcastAnnouncementRepository.saveAndFlush(
                broadcastAnnouncementPersistenceMapper.toEntity(announcement)
        );
        return broadcastAnnouncementPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<BroadcastAnnouncement> findById(UUID broadcastId) {
        return broadcastAnnouncementRepository.findById(broadcastId)
                .map(broadcastAnnouncementPersistenceMapper::toDomain);
    }

    @Override
    public List<BroadcastAnnouncement> findAll(BroadcastAnnouncementStatus status) {
        List<BroadcastAnnouncementEntity> entities = status == null
                ? broadcastAnnouncementRepository.findAllByOrderByCreatedAtDescBroadcastIdDesc()
                : broadcastAnnouncementRepository.findByStatusOrderByCreatedAtDescBroadcastIdDesc(status);
        return broadcastAnnouncementPersistenceMapper.toDomains(entities);
    }

    @Override
    public List<BroadcastAnnouncement> findActivePublishedForRole(String roleCode, Instant now) {
        List<BroadcastAnnouncementEntity> entities = broadcastAnnouncementRepository.findActivePublishedAnnouncements(now);
        return entities.stream()
                .filter(entity -> isVisibleToRole(entity, roleCode))
                .map(broadcastAnnouncementPersistenceMapper::toDomain)
                .toList();
    }

    private boolean isVisibleToRole(BroadcastAnnouncementEntity entity, String roleCode) {
        if (entity.getAudienceType() == BroadcastAnnouncementAudienceType.ALL_ACTIVE_ACCOUNTS) {
            return true;
        }
        return roleCode != null
                && entity.getRoleCodes() != null
                && entity.getRoleCodes().contains(roleCode);
    }

    @Override
    public boolean existsByTitle(String title) {
        return broadcastAnnouncementRepository.existsByTitleIgnoreCase(title);
    }

    @Override
    public boolean existsByTitleAndBroadcastIdNot(String title, UUID broadcastId) {
        return broadcastAnnouncementRepository.existsByTitleIgnoreCaseAndBroadcastIdNot(title, broadcastId);
    }

    @Override
    public void deleteById(UUID broadcastId) {
        broadcastAnnouncementRepository.deleteById(broadcastId);
        broadcastAnnouncementRepository.flush();
    }
}
