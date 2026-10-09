package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglayout.port.out.ParkingLayoutPortOut;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutElement;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingLayoutVersion;
import com.ban.vehicle_management.domain.parking.parkinglayout.model.ParkingSpaceLayoutItem;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLayoutElementPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLayoutVersionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingSpaceLayoutItemPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLayoutElementRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLayoutVersionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSpaceLayoutItemRepository;
import com.ban.vehicle_management.shared.enumeration.parking.LayoutVersionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ParkingLayoutPersistenceAdapter implements ParkingLayoutPortOut {

    private final ParkingLayoutVersionRepository versionRepository;
    private final ParkingSpaceLayoutItemRepository layoutItemRepository;
    private final ParkingLayoutElementRepository layoutElementRepository;
    private final ParkingLayoutVersionPersistenceMapper versionMapper;
    private final ParkingSpaceLayoutItemPersistenceMapper layoutItemMapper;
    private final ParkingLayoutElementPersistenceMapper layoutElementMapper;

    @Override
    @Transactional
    public ParkingLayoutVersion saveLayoutVersion(ParkingLayoutVersion layoutVersion) {
        var entity = versionMapper.toEntity(layoutVersion);
        var saved = versionRepository.save(entity);
        return versionMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingLayoutVersion> findLayoutVersionById(UUID layoutVersionId) {
        return versionRepository.findById(layoutVersionId).map(versionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingLayoutVersion> findDraftByZoneId(UUID zoneId) {
        return versionRepository
                .findFirstByZoneIdAndStatusOrderByVersionDesc(zoneId, LayoutVersionStatus.DRAFT)
                .map(versionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParkingLayoutVersion> findPublishedByZoneId(UUID zoneId) {
        return versionRepository
                .findFirstByZoneIdAndStatusOrderByVersionDesc(zoneId, LayoutVersionStatus.PUBLISHED)
                .map(versionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLayoutVersion> findAllByZoneId(UUID zoneId) {
        return versionRepository.findByZoneIdOrderByVersionDesc(zoneId).stream().map(versionMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<ParkingSpaceLayoutItem> saveLayoutItems(List<ParkingSpaceLayoutItem> items) {
        var entities = items.stream().map(layoutItemMapper::toEntity).toList();
        var saved = layoutItemRepository.saveAll(entities);
        return saved.stream().map(layoutItemMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingSpaceLayoutItem> findLayoutItemsByVersionId(UUID layoutVersionId) {
        return layoutItemRepository.findByLayoutVersionId(layoutVersionId).stream().map(layoutItemMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteLayoutItemsByVersionId(UUID layoutVersionId) {
        layoutItemRepository.deleteByLayoutVersionId(layoutVersionId);
    }

    @Override
    @Transactional
    public List<ParkingLayoutElement> saveLayoutElements(List<ParkingLayoutElement> elements) {
        var entities = elements.stream().map(layoutElementMapper::toEntity).toList();
        var saved = layoutElementRepository.saveAll(entities);
        return saved.stream().map(layoutElementMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLayoutElement> findLayoutElementsByVersionId(UUID layoutVersionId) {
        return layoutElementRepository.findByLayoutVersionIdOrderByDisplayOrder(layoutVersionId).stream().map(layoutElementMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteLayoutElementsByVersionId(UUID layoutVersionId) {
        layoutElementRepository.deleteByLayoutVersionId(layoutVersionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPublishedLayoutForZone(UUID zoneId) {
        return versionRepository
                .findFirstByZoneIdAndStatusOrderByVersionDesc(zoneId, LayoutVersionStatus.PUBLISHED)
                .isPresent();
    }
}