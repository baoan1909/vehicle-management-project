package com.ban.vehicle_management.application.hardware.device.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.hardware.device.port.out.DevicePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private DevicePortOut devicePortOut;

    @Mock
    private ParkingLotPortOut parkingLotPortOut;

    @InjectMocks
    private DeviceUseCaseImpl useCase;

    @Test
    void shouldCreateDeviceWhenValid() {
        UUID parkingLotId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        Device request = validDevice(parkingLotId, laneId);
        request.setDeviceCode(" cam-main-01 ");

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsLaneInParkingLot(laneId, parkingLotId))
                .thenReturn(true);
        when(devicePortOut.existsByDeviceCode("CAM-MAIN-01"))
                .thenReturn(false);
        when(devicePortOut.save(request))
                .thenReturn(request);

        Device result = useCase.createDevice(request);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_CREATE_ALL");
        assertNotNull(result.getDeviceId());
        assertEquals("CAM-MAIN-01", result.getDeviceCode());
        assertEquals(DeviceStatus.ACTIVE, result.getStatus());
        verify(devicePortOut).save(request);
    }

    @Test
    void shouldRejectCreateWhenParkingLotNotFound() {
        UUID parkingLotId = UUID.randomUUID();
        Device request = validDevice(parkingLotId, null);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.createDevice(request)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldRejectCreateWhenParkingLotClosed() {
        UUID parkingLotId = UUID.randomUUID();
        Device request = validDevice(parkingLotId, null);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.CLOSED)));

        assertThrows(
                ConflictException.class,
                () -> useCase.createDevice(request)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldRejectCreateWhenLaneDoesNotBelongToParkingLot() {
        UUID parkingLotId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        Device request = validDevice(parkingLotId, laneId);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsLaneInParkingLot(laneId, parkingLotId))
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> useCase.createDevice(request)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldRejectCreateWhenDeviceCodeAlreadyExists() {
        UUID parkingLotId = UUID.randomUUID();
        Device request = validDevice(parkingLotId, null);

        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsByDeviceCode("CAM-01"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> useCase.createDevice(request)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldReturnDeviceById() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));

        Device result = useCase.getDeviceById(deviceId);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_READ_ALL");
        assertEquals(deviceId, result.getDeviceId());
    }

    @Test
    void shouldThrowWhenDeviceNotFound() {
        UUID deviceId = UUID.randomUUID();

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> useCase.getDeviceById(deviceId)
        );
    }

    @Test
    void shouldTrimKeywordWhenListingDevices() {
        UUID parkingLotId = UUID.randomUUID();

        when(devicePortOut.findAll(
                parkingLotId,
                null,
                DeviceType.CAMERA,
                DeviceStatus.ACTIVE,
                "Camera"
        )).thenReturn(List.of(new Device()));

        List<Device> result = useCase.getDevices(
                parkingLotId,
                null,
                DeviceType.CAMERA,
                DeviceStatus.ACTIVE,
                "  Camera  "
        );

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_READ_ALL");
        assertEquals(1, result.size());
    }

    @Test
    void shouldUpdateMutableFieldsAndKeepStatus() {
        UUID deviceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        Device existing = validDevice(parkingLotId, null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.MAINTENANCE);

        Device request = validDevice(parkingLotId, null);
        request.setDeviceCode("cam-new-01");
        request.setName(" Camera moi ");
        request.setStatus(DeviceStatus.ACTIVE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsByDeviceCodeAndDeviceIdNot(
                "CAM-NEW-01",
                deviceId
        )).thenReturn(false);
        when(devicePortOut.save(existing))
                .thenReturn(existing);

        Device result = useCase.updateDevice(deviceId, request);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_UPDATE_ALL");
        assertEquals("CAM-NEW-01", result.getDeviceCode());
        assertEquals("Camera moi", result.getName());
        assertEquals(DeviceStatus.MAINTENANCE, result.getStatus());
    }

    @Test
    void shouldRejectUpdateWhenDeviceCodeUsedByOtherDevice() {
        UUID deviceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        Device existing = validDevice(parkingLotId, null);
        existing.setDeviceId(deviceId);
        Device request = validDevice(parkingLotId, null);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsByDeviceCodeAndDeviceIdNot(
                "CAM-01",
                deviceId
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> useCase.updateDevice(deviceId, request)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldActivateDeviceWhenParentResourcesAreOperational() {
        UUID deviceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        Device existing = validDevice(parkingLotId, laneId);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.OFFLINE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsNonClosedLaneById(laneId))
                .thenReturn(true);
        when(devicePortOut.save(existing))
                .thenReturn(existing);

        Device result = useCase.activateDevice(deviceId);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_STATUS_UPDATE_ALL");
        assertEquals(DeviceStatus.ACTIVE, result.getStatus());
    }

    @Test
    void shouldReturnExistingDeviceWhenAlreadyActive() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.ACTIVE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));

        Device result = useCase.activateDevice(deviceId);

        assertSame(existing, result);
        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldRejectActivateWhenLaneClosed() {
        UUID deviceId = UUID.randomUUID();
        UUID parkingLotId = UUID.randomUUID();
        UUID laneId = UUID.randomUUID();
        Device existing = validDevice(parkingLotId, laneId);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.OFFLINE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(parkingLotPortOut.findById(parkingLotId))
                .thenReturn(Optional.of(parkingLot(ParkingLotStatus.ACTIVE)));
        when(devicePortOut.existsNonClosedLaneById(laneId))
                .thenReturn(false);

        assertThrows(
                ConflictException.class,
                () -> useCase.activateDevice(deviceId)
        );

        verify(devicePortOut, never()).save(any(Device.class));
    }

    @Test
    void shouldMarkDeviceOffline() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.ACTIVE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(devicePortOut.save(existing))
                .thenReturn(existing);

        Device result = useCase.markDeviceOffline(deviceId);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_STATUS_UPDATE_ALL");
        assertEquals(DeviceStatus.OFFLINE, result.getStatus());
    }

    @Test
    void shouldRejectOfflineWhenDeviceRetired() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.RETIRED);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));

        assertThrows(
                ConflictException.class,
                () -> useCase.markDeviceOffline(deviceId)
        );
    }

    @Test
    void shouldMarkDeviceMaintenance() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.ACTIVE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));
        when(devicePortOut.save(existing))
                .thenReturn(existing);

        Device result = useCase.markDeviceMaintenance(deviceId);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_STATUS_UPDATE_ALL");
        assertEquals(DeviceStatus.MAINTENANCE, result.getStatus());
    }

    @Test
    void shouldRetireDeviceWhenDeleting() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.ACTIVE);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));

        useCase.deleteDevice(deviceId);

        verify(currentAccountPortIn)
                .requirePermission("DEVICE_DELETE_ALL");
        assertEquals(DeviceStatus.RETIRED, existing.getStatus());
        verify(devicePortOut).save(existing);
    }

    @Test
    void shouldDoNothingWhenDeletingAlreadyRetiredDevice() {
        UUID deviceId = UUID.randomUUID();
        Device existing = validDevice(UUID.randomUUID(), null);
        existing.setDeviceId(deviceId);
        existing.setStatus(DeviceStatus.RETIRED);

        when(devicePortOut.findById(deviceId))
                .thenReturn(Optional.of(existing));

        useCase.deleteDevice(deviceId);

        verify(devicePortOut, never()).save(any(Device.class));
    }

    private Device validDevice(UUID parkingLotId, UUID laneId) {
        Device device = new Device();
        device.setParkingLotId(parkingLotId);
        device.setLaneId(laneId);
        device.setDeviceCode("CAM-01");
        device.setDeviceType(DeviceType.CAMERA);
        device.setName("Camera cong chinh");
        device.setIpAddress("192.168.1.10");
        device.setStatus(DeviceStatus.ACTIVE);
        return device;
    }

    private ParkingLot parkingLot(ParkingLotStatus status) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setParkingLotId(UUID.randomUUID());
        parkingLot.setCode("HCMUTE");
        parkingLot.setName("HCMUTE Parking");
        parkingLot.setTotalCapacity(1000);
        parkingLot.setStatus(status);
        return parkingLot;
    }
}
