package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.VehicleTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.CardRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.catalog.VehicleTypeSpecifications;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class VehicleTypePersistenceAdapter implements VehicleTypePortOut {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final PriceRuleRepository priceRuleRepository;
    private final CustomerVehicleRepository customerVehicleRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final CardRepository cardRepository;
    private final ZoneRepository zoneRepository;
    private final VehicleTypePersistenceMapper vehicleTypePersistenceMapper;

    public VehicleTypePersistenceAdapter(
            VehicleTypeRepository vehicleTypeRepository,
            PriceRuleRepository priceRuleRepository,
            CustomerVehicleRepository customerVehicleRepository,
            ParkingSessionRepository parkingSessionRepository,
            CardRepository cardRepository,
            ZoneRepository zoneRepository,
            VehicleTypePersistenceMapper vehicleTypePersistenceMapper
    ) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.priceRuleRepository = priceRuleRepository;
        this.customerVehicleRepository = customerVehicleRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.cardRepository = cardRepository;
        this.zoneRepository = zoneRepository;
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
        Specification<VehicleTypeEntity> specification = VehicleTypeSpecifications.withFilters(isActive);
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
    public boolean existsByCodeAndVehicleTypeIdNot(String code, UUID vehicleTypeId) {
        return vehicleTypeRepository.existsByCodeAndVehicleTypeIdNot(code, vehicleTypeId);
    }

    @Override
    public boolean hasActivePriceRules(UUID vehicleTypeId) {
        return priceRuleRepository.existsByVehicleTypeIdAndIsActiveTrue(vehicleTypeId);
    }

    @Override
    public boolean hasActiveCustomerVehicles(UUID vehicleTypeId) {
        return customerVehicleRepository.existsByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                List.of(CustomerVehicleStatus.ACTIVE, CustomerVehicleStatus.BLOCKED)
        );
    }

    @Override
    public boolean hasOpenParkingSessions(UUID vehicleTypeId) {
        return parkingSessionRepository.existsByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                List.of(ParkingSessionStatus.OPEN, ParkingSessionStatus.LOST_CARD)
        );
    }

    @Override
    public boolean hasActiveCards(UUID vehicleTypeId) {
        return cardRepository.existsByVehicleTypeIdAndStatusIn(
                vehicleTypeId,
                List.of(
                        CardStatus.AVAILABLE,
                        CardStatus.ASSIGNED,
                        CardStatus.IN_USE,
                        CardStatus.BLOCKED,
                        CardStatus.LOST,
                        CardStatus.DAMAGED
                )
        );
    }

    @Override
    public boolean hasActiveZones(UUID vehicleTypeId) {
        return zoneRepository.existsByVehicleTypeIdAndStatus(vehicleTypeId, ZoneStatus.ACTIVE);
    }
}



