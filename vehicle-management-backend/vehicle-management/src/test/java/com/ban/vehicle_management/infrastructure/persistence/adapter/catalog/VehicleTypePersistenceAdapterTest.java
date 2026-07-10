package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.infrastructure.mapper.catalog.VehicleTypePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VehicleTypeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.PriceRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ZoneRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerVehicleRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleTypePersistenceAdapterTest {

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @Mock
    private PriceRuleRepository priceRuleRepository;

    @Mock
    private CustomerVehicleRepository customerVehicleRepository;

    @Mock
    private ParkingSessionRepository parkingSessionRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private VehicleTypePersistenceMapper vehicleTypePersistenceMapper;

    @InjectMocks
    private VehicleTypePersistenceAdapter vehicleTypePersistenceAdapter;

    @Test
    void shouldUseSaveAndFlushWhenSavingVehicleType() {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setVehicleTypeId(UUID.randomUUID());

        VehicleTypeEntity vehicleTypeEntity = new VehicleTypeEntity();

        when(vehicleTypePersistenceMapper.toEntity(vehicleType)).thenReturn(vehicleTypeEntity);
        when(vehicleTypeRepository.saveAndFlush(vehicleTypeEntity)).thenReturn(vehicleTypeEntity);
        when(vehicleTypePersistenceMapper.toDomain(vehicleTypeEntity)).thenReturn(vehicleType);

        vehicleTypePersistenceAdapter.save(vehicleType);

        verify(vehicleTypeRepository).saveAndFlush(vehicleTypeEntity);
    }
}
