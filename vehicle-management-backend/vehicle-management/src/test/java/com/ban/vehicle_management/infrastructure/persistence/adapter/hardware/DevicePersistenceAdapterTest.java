package com.ban.vehicle_management.infrastructure.persistence.adapter.hardware;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.infrastructure.mapper.hardware.DevicePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.hardware.DeviceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.LaneRepository;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import com.ban.vehicle_management.shared.enumeration.parking.LaneStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DevicePersistenceAdapterTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private DevicePersistenceMapper devicePersistenceMapper;

    @InjectMocks
    private DevicePersistenceAdapter adapter;

    @Test
    void shouldSaveAndFlushDevice() {
        Device domain = new Device();
        DeviceEntity entity = new DeviceEntity();

        when(devicePersistenceMapper.toEntity(domain))
                .thenReturn(entity);
        when(deviceRepository.saveAndFlush(entity))
                .thenReturn(entity);
        when(devicePersistenceMapper.toDomain(entity))
                .thenReturn(domain);

        Device result = adapter.save(domain);

        assertSame(domain, result);
        verify(deviceRepository).saveAndFlush(entity);
    }

    @Test
    void shouldFindDeviceById() {
        UUID deviceId = UUID.randomUUID();
        DeviceEntity entity = new DeviceEntity();
        Device domain = new Device();
        domain.setDeviceId(deviceId);

        when(deviceRepository.findById(deviceId))
                .thenReturn(Optional.of(entity));
        when(devicePersistenceMapper.toDomain(entity))
                .thenReturn(domain);

        Optional<Device> result = adapter.findById(deviceId);

        assertTrue(result.isPresent());
        assertEquals(deviceId, result.get().getDeviceId());
    }

    @Test
    void shouldReturnEmptyWhenDeviceNotFound() {
        UUID deviceId = UUID.randomUUID();

        when(deviceRepository.findById(deviceId))
                .thenReturn(Optional.empty());

        Optional<Device> result = adapter.findById(deviceId);

        assertTrue(result.isEmpty());
        verify(devicePersistenceMapper, never())
                .toDomain(any(DeviceEntity.class));
    }

    @Test
    void shouldReturnMappedFilteredList() {
        UUID parkingLotId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        DeviceEntity entity = new DeviceEntity();
        Device domain = new Device();

        when(deviceRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(entity));
        when(devicePersistenceMapper.toDomain(entity))
                .thenReturn(domain);

        List<Device> result = adapter.findAll(
                parkingLotId,
                laneId,
                DeviceType.CAMERA,
                DeviceStatus.ACTIVE,
                "Camera"
        );

        assertEquals(1, result.size());
        assertSame(domain, result.get(0));
    }

    @Test
    void shouldCheckDeviceCodeExists() {
        when(deviceRepository.existsByDeviceCode("CAM-01"))
                .thenReturn(true);

        boolean result = adapter.existsByDeviceCode("CAM-01");

        assertTrue(result);
    }

    @Test
    void shouldCheckDeviceCodeUsedByOtherDevice() {
        UUID deviceId = UUID.randomUUID();

        when(deviceRepository.existsByDeviceCodeAndDeviceIdNot(
                "CAM-01",
                deviceId
        )).thenReturn(true);

        boolean result = adapter.existsByDeviceCodeAndDeviceIdNot(
                "CAM-01",
                deviceId
        );

        assertTrue(result);
    }

    @Test
    void shouldCheckLaneBelongsToParkingLot() {
        UUID laneId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();

        when(laneRepository.existsLaneInParkingLot(laneId, parkingLotId))
                .thenReturn(true);

        boolean result = adapter.existsLaneInParkingLot(laneId, parkingLotId);

        assertTrue(result);
    }

    @Test
    void shouldCheckNonClosedLaneById() {
        UUID laneId = UUID.randomUUID();

        when(laneRepository.existsByLaneIdAndStatusNot(
                laneId,
                LaneStatus.CLOSED
        )).thenReturn(true);

        boolean result = adapter.existsNonClosedLaneById(laneId);

        assertTrue(result);
        verify(laneRepository).existsByLaneIdAndStatusNot(
                laneId,
                LaneStatus.CLOSED
        );
    }
}
