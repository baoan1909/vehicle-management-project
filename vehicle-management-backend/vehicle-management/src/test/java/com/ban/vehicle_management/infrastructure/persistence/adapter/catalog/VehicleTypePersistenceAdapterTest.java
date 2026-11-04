package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.ban.vehicle_management.shared.enumeration.parking.ZoneStatus;
import java.util.List;
import java.util.Set;
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
    private SubscriptionRepository subscriptionRepository;

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

    @Test
    void shouldRemoveDeactivatedTypeAndKeepMultiTypeZoneActive() {
        UUID deactivatedTypeId = UUID.randomUUID();
        UUID remainingTypeId = UUID.randomUUID();
        ZoneEntity zone = new ZoneEntity();
        zone.setVehicleTypeId(deactivatedTypeId);
        zone.setVehicleTypeIds(Set.of(deactivatedTypeId, remainingTypeId));
        zone.setStatus(ZoneStatus.ACTIVE);
        when(zoneRepository.findByVehicleTypeIdsContains(deactivatedTypeId)).thenReturn(List.of(zone));

        int affectedZoneCount = vehicleTypePersistenceAdapter.detachVehicleTypeFromZones(deactivatedTypeId);

        assertEquals(1, affectedZoneCount);
        assertEquals(Set.of(remainingTypeId), zone.getVehicleTypeIds());
        assertEquals(remainingTypeId, zone.getVehicleTypeId());
        assertEquals(ZoneStatus.ACTIVE, zone.getStatus());
        verify(zoneRepository).saveAll(List.of(zone));
    }

    @Test
    void shouldMoveSingleTypeZoneToMaintenanceWhenTypeIsRemoved() {
        UUID deactivatedTypeId = UUID.randomUUID();
        ZoneEntity zone = new ZoneEntity();
        zone.setVehicleTypeId(deactivatedTypeId);
        zone.setVehicleTypeIds(Set.of(deactivatedTypeId));
        zone.setStatus(ZoneStatus.ACTIVE);
        when(zoneRepository.findByVehicleTypeIdsContains(deactivatedTypeId)).thenReturn(List.of(zone));

        vehicleTypePersistenceAdapter.detachVehicleTypeFromZones(deactivatedTypeId);

        assertEquals(Set.of(), zone.getVehicleTypeIds());
        assertNull(zone.getVehicleTypeId());
        assertEquals(ZoneStatus.MAINTENANCE, zone.getStatus());
        verify(zoneRepository).saveAll(List.of(zone));
    }

    @Test
    void shouldDetachVehicleTypeOnlyFromSelectedParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        ZoneEntity zone = new ZoneEntity();
        zone.setVehicleTypeId(vehicleTypeId);
        zone.setVehicleTypeIds(Set.of(vehicleTypeId));
        zone.setStatus(ZoneStatus.ACTIVE);
        when(zoneRepository.findByParkingLotIdAndVehicleTypeIdsContains(parkingLotId, vehicleTypeId))
                .thenReturn(List.of(zone));

        vehicleTypePersistenceAdapter.detachVehicleTypeFromZones(parkingLotId, vehicleTypeId);

        assertEquals(ZoneStatus.MAINTENANCE, zone.getStatus());
        verify(zoneRepository).findByParkingLotIdAndVehicleTypeIdsContains(parkingLotId, vehicleTypeId);
        verify(zoneRepository).saveAll(List.of(zone));
    }
}
