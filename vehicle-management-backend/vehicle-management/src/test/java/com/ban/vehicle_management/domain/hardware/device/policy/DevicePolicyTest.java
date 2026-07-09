package com.ban.vehicle_management.domain.hardware.device.policy;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DevicePolicyTest {

    private final DevicePolicy policy = new DevicePolicy();

    @Test
    void shouldInitializeNewDeviceAndNormalizeEditableFields() {
        Device device = validDevice();
        device.setDeviceCode(" cam-main-01 ");
        device.setName("  Camera cong chinh  ");
        device.setIpAddress(" 192.168.1.10 ");
        device.setStatus(DeviceStatus.OFFLINE);

        policy.initializeNew(device);

        assertNotNull(device.getDeviceId());
        assertEquals("CAM-MAIN-01", device.getDeviceCode());
        assertEquals("Camera cong chinh", device.getName());
        assertEquals("192.168.1.10", device.getIpAddress());
        assertEquals(DeviceStatus.ACTIVE, device.getStatus());
    }

    @Test
    void shouldRejectMissingParkingLotId() {
        Device device = validDevice();
        device.setParkingLotId(null);

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(device)
        );
    }

    @Test
    void shouldRejectMissingDeviceType() {
        Device device = validDevice();
        device.setDeviceType(null);

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(device)
        );
    }

    @Test
    void shouldRejectBlankDeviceCode() {
        Device device = validDevice();
        device.setDeviceCode(" ");

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(device)
        );
    }

    @Test
    void shouldRejectInvalidDeviceCode() {
        Device device = validDevice();
        device.setDeviceCode("CAM MAIN 01");

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(device)
        );
    }

    @Test
    void shouldRejectBlankName() {
        Device device = validDevice();
        device.setName(" ");

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeNew(device)
        );
    }

    @Test
    void shouldActivateDevice() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.MAINTENANCE);

        policy.activate(device);

        assertEquals(DeviceStatus.ACTIVE, device.getStatus());
    }

    @Test
    void shouldMarkDeviceOfflineWhenNotRetired() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.ACTIVE);

        policy.offline(device);

        assertEquals(DeviceStatus.OFFLINE, device.getStatus());
    }

    @Test
    void shouldMarkDeviceMaintenanceWhenNotRetired() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.ACTIVE);

        policy.maintenance(device);

        assertEquals(DeviceStatus.MAINTENANCE, device.getStatus());
    }

    @Test
    void shouldRejectOfflineWhenDeviceRetired() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.RETIRED);

        assertThrows(
                ConflictException.class,
                () -> policy.offline(device)
        );
    }

    @Test
    void shouldRejectMaintenanceWhenDeviceRetired() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.RETIRED);

        assertThrows(
                ConflictException.class,
                () -> policy.maintenance(device)
        );
    }

    @Test
    void shouldRetireDevice() {
        Device device = validDevice();
        device.setStatus(DeviceStatus.ACTIVE);

        policy.retire(device);

        assertEquals(DeviceStatus.RETIRED, device.getStatus());
    }

    private Device validDevice() {
        Device device = new Device();
        device.setDeviceId(UUID.randomUUID());
        device.setParkingLotId(UUID.randomUUID());
        device.setLaneId(UUID.randomUUID());
        device.setDeviceCode("CAM-01");
        device.setDeviceType(DeviceType.CAMERA);
        device.setName("Camera cong chinh");
        device.setIpAddress("192.168.1.10");
        device.setStatus(DeviceStatus.ACTIVE);
        device.setConfig(Map.of("streamUrl", "rtsp://localhost/cam-01"));
        return device;
    }
}
