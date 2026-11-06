package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.VehicleTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ZoneEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.VehicleTypeSpecifications;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class VehicleTypePersistenceAdapter implements VehicleTypePortOut {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final CustomerVehicleRepository customerVehicleRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final ZoneRepository zoneRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VehicleTypePersistenceMapper vehicleTypePersistenceMapper;

    public VehicleTypePersistenceAdapter(
            VehicleTypeRepository vehicleTypeRepository,
            PriceRuleRepository priceRuleRepository,
            CustomerVehicleRepository customerVehicleRepository,
            ParkingSessionRepository parkingSessionRepository,
            ZoneRepository zoneRepository,
            SubscriptionRepository subscriptionRepository,
            VehicleTypePersistenceMapper vehicleTypePersistenceMapper
    ) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.customerVehicleRepository = customerVehicleRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.zoneRepository = zoneRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.vehicleTypePersistenceMapper = vehicleTypePersistenceMapper;
    }

    @Override
    public VehicleType save(VehicleType vehicleType) {
        VehicleTypeEntity vehicleTypeEntity = vehicleTypePersistenceMapper.toEntity(vehicleType);
        VehicleTypeEntity savedVehicleTypeEntity = vehicleTypeRepository.saveAndFlush(vehicleTypeEntity);
        return vehicleTypePersistenceMapper.toDomain(savedVehicleTypeEntity);
    }

    @Override
    public Optional<VehicleType> findById(UUID vehicleTypeId) {
        return vehicleTypeRepository.findById(vehicleTypeId)
                .map(vehicleTypePersistenceMapper::toDomain);
    }

    @Override
    public List<VehicleType> findAll(Boolean isActive) {
        return findAll(isActive, null);
    }

    @Override
    public List<VehicleType> findAll(Boolean isActive, Set<UUID> organizationIds) {
        Specification<VehicleTypeEntity> specification = VehicleTypeSpecifications.withFilters(isActive, organizationIds);
        List<VehicleTypeEntity> vehicleTypeEntities = vehicleTypeRepository.findAll(specification);

        return vehicleTypeEntities.stream()
                .map(vehicleTypePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return vehicleTypeRepository.existsByCode(code);
    }

    @Override
    public boolean existsByCodeInOrganization(String code, UUID organizationId) {
        return vehicleTypeRepository.existsByCodeAndOrganizationId(code, organizationId);
    }

    @Override
    public boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId) {
        return vehicleTypeRepository.existsByCodeAndVehicleTypeIdNot(code, vehicleTypeId);
    }

    @Override
    public boolean existsByCodeInOrganizationExcludingId(String code, UUID organizationId, UUID vehicleTypeId) {
        return vehicleTypeRepository.existsByCodeAndOrganizationIdAndVehicleTypeIdNot(code, organizationId, vehicleTypeId);
    }

    @Override
    public long countActivePriceRules(UUID vehicleTypeId) {
        return priceRuleRepository.countByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Override
    public long countActiveCustomerVehicles(UUID vehicleTypeId) {
        return customerVehicleRepository.countByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                List.of(CustomerVehicleStatus.ACTIVE, CustomerVehicleStatus.BLOCKED)
        );
    }

    @Override
    public long countOpenParkingSessions(UUID vehicleTypeId) {
        return parkingSessionRepository.countByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                List.of(ParkingSessionStatus.OPEN, ParkingSessionStatus.LOST_CARD)
        );
    }

    @Override
    public long countAssignedZones(UUID vehicleTypeId) {
        return zoneRepository.countByVehicleTypeIdsContains(vehicleTypeId);
    }

    @Override
    public int deactivateActivePriceRules(UUID vehicleTypeId) {
        return priceRuleRepository.deactivateActiveByVehicleTypeId(vehicleTypeId);
    }

    @Override
    public int detachVehicleTypeFromZones(UUID vehicleTypeId) {
        List<ZoneEntity> affectedZones = zoneRepository.findByVehicleTypeIdsContains(vehicleTypeId);
        return detachVehicleTypeFromZones(affectedZones, vehicleTypeId);
    }

    @Override
    public long countServiceObligationSubscriptions(UUID parkingLotId, UUID vehicleTypeId, LocalDate businessDate) {
        return subscriptionRepository.countServiceObligations(
                parkingLotId,
                vehicleTypeId,
                businessDate,
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING_CARD,
                        SubscriptionStatus.PENDING_SPACE)
        );
    }

    @Override
    public long countServiceObligationSubscriptions(UUID vehicleTypeId, LocalDate businessDate) {
        return subscriptionRepository.countServiceObligations(
                vehicleTypeId,
                businessDate,
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING_CARD,
                        SubscriptionStatus.PENDING_SPACE)
        );
    }

    @Override
    public long countOpenParkingSessions(UUID parkingLotId, UUID vehicleTypeId) {
        return parkingSessionRepository.countByParkingLotIdAndVehicleTypeIdAndStatusIn(
                parkingLotId,
                vehicleTypeId,
                List.of(ParkingSessionStatus.OPEN, ParkingSessionStatus.LOST_CARD)
        );
    }

    @Override
    public int detachVehicleTypeFromZones(UUID parkingLotId, UUID vehicleTypeId) {
        List<ZoneEntity> affectedZones = zoneRepository
                .findByParkingLotIdAndVehicleTypeIdsContains(parkingLotId, vehicleTypeId);
        return detachVehicleTypeFromZones(affectedZones, vehicleTypeId);
    }

    private int detachVehicleTypeFromZones(List<ZoneEntity> affectedZones, UUID vehicleTypeId) {
        for (ZoneEntity zone : affectedZones) {
            Set<UUID> remainingVehicleTypeIds = zone.getVehicleTypeIds() == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(zone.getVehicleTypeIds());
            remainingVehicleTypeIds.remove(vehicleTypeId);
            zone.setVehicleTypeIds(remainingVehicleTypeIds);

            if (vehicleTypeId.equals(zone.getVehicleTypeId())) {
                zone.setVehicleTypeId(remainingVehicleTypeIds.stream().findFirst().orElse(null));
            }
            if (remainingVehicleTypeIds.isEmpty() && zone.getStatus() != ZoneStatus.CLOSED) {
                zone.setStatus(ZoneStatus.MAINTENANCE);
            }
        }
        zoneRepository.saveAll(affectedZones);
        return affectedZones.size();
    }
}



