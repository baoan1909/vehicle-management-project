package com.ban.vehicle_management.application.hardware.device.usecase;

import com.ban.vehicle_management.application.hardware.device.port.in.DevicePortIn;
import com.ban.vehicle_management.application.hardware.device.port.out.DevicePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.domain.hardware.device.policy.DevicePolicy;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceType;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceUseCaseImpl implements DevicePortIn {

    private static final String DEVICE_CREATE_ALL = "DEVICE_CREATE_ALL";
    private static final String DEVICE_READ_ALL = "DEVICE_READ_ALL";
    private static final String DEVICE_UPDATE_ALL = "DEVICE_UPDATE_ALL";
    private static final String DEVICE_STATUS_UPDATE_ALL = "DEVICE_STATUS_UPDATE_ALL";
    private static final String DEVICE_DELETE_ALL = "DEVICE_DELETE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final DevicePortOut devicePortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final DevicePolicy devicePolicy = new DevicePolicy();

    public DeviceUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            DevicePortOut devicePortOut,
            ParkingLotPortOut parkingLotPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.devicePortOut = devicePortOut;
        this.parkingLotPortOut = parkingLotPortOut;
    }

    @Override
    @Transactional
    public Device createDevice(Device device) {
        currentAccountPortIn.requirePermission(DEVICE_CREATE_ALL);

        devicePolicy.initializeNew(device);
        ensureParkingLotAvailable(device.getParkingLotId());
        ensureLaneBelongsToParkingLotIfPresent(device);
        ensureDeviceCodeNotExists(device.getDeviceCode());

        return devicePortOut.save(device);
    }

    @Override
    @Transactional(readOnly = true)
    public Device getDeviceById(UUID deviceId) {
        currentAccountPortIn.requirePermission(DEVICE_READ_ALL);
        return findExistingDevice(deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> getDevices(
            UUID parkingLotId,
            UUID laneId,
            DeviceType deviceType,
            DeviceStatus status,
            String keyword
    ) {
        currentAccountPortIn.requirePermission(DEVICE_READ_ALL);

        return devicePortOut.findAll(
                parkingLotId,
                laneId,
                deviceType,
                status,
                normalizeKeyword(keyword)
        );
    }

    @Override
    @Transactional
    public Device updateDevice(UUID deviceId, Device request) {
        currentAccountPortIn.requirePermission(DEVICE_UPDATE_ALL);

        Device existing = findExistingDevice(deviceId);

        request.setDeviceId(existing.getDeviceId());
        request.setStatus(existing.getStatus());

        devicePolicy.validateForUpdate(request);
        ensureParkingLotAvailable(request.getParkingLotId());
        ensureLaneBelongsToParkingLotIfPresent(request);
        ensureDeviceCodeNotUsedByOtherDevice(
                request.getDeviceCode(),
                existing.getDeviceId()
        );

        existing.setParkingLotId(request.getParkingLotId());
        existing.setLaneId(request.getLaneId());
        existing.setDeviceCode(request.getDeviceCode());
        existing.setDeviceType(request.getDeviceType());
        existing.setName(request.getName());
        existing.setIpAddress(request.getIpAddress());
        existing.setConfig(request.getConfig());

        return devicePortOut.save(existing);
    }

    @Override
    @Transactional
    public Device activateDevice(UUID deviceId) {
        currentAccountPortIn.requirePermission(DEVICE_STATUS_UPDATE_ALL);

        Device existing = findExistingDevice(deviceId);

        if (existing.getStatus() == DeviceStatus.ACTIVE) {
            return existing;
        }

        ensureParkingLotAvailable(existing.getParkingLotId());
        ensureLaneOperationalIfPresent(existing);

        devicePolicy.activate(existing);

        return devicePortOut.save(existing);
    }

    @Override
    @Transactional
    public Device markDeviceOffline(UUID deviceId) {
        currentAccountPortIn.requirePermission(DEVICE_STATUS_UPDATE_ALL);

        Device existing = findExistingDevice(deviceId);

        if (existing.getStatus() == DeviceStatus.OFFLINE) {
            return existing;
        }

        devicePolicy.offline(existing);

        return devicePortOut.save(existing);
    }

    @Override
    @Transactional
    public Device markDeviceMaintenance(UUID deviceId) {
        currentAccountPortIn.requirePermission(DEVICE_STATUS_UPDATE_ALL);

        Device existing = findExistingDevice(deviceId);

        if (existing.getStatus() == DeviceStatus.MAINTENANCE) {
            return existing;
        }

        devicePolicy.maintenance(existing);

        return devicePortOut.save(existing);
    }

    @Override
    @Transactional
    public void deleteDevice(UUID deviceId) {
        currentAccountPortIn.requirePermission(DEVICE_DELETE_ALL);

        Device existing = findExistingDevice(deviceId);

        if (existing.getStatus() == DeviceStatus.RETIRED) {
            return;
        }

        devicePolicy.retire(existing);
        devicePortOut.save(existing);
    }

    private Device findExistingDevice(UUID deviceId) {
        return devicePortOut.findById(deviceId)
                .orElseThrow(() ->
                        new NotFoundException("Device not found")
                );
    }

    private void ensureParkingLotAvailable(UUID parkingLotId) {
        ParkingLot parkingLot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() ->
                        new NotFoundException("Parking lot not found")
                );

        if (parkingLot.getStatus() == ParkingLotStatus.CLOSED) {
            throw new ConflictException(
                    "Cannot use device for a closed parking lot"
            );
        }
    }

    private void ensureLaneBelongsToParkingLotIfPresent(Device device) {
        if (device.getLaneId() == null) {
            return;
        }

        boolean exists = devicePortOut.existsLaneInParkingLot(
                device.getLaneId(),
                device.getParkingLotId()
        );

        if (!exists) {
            throw new ConflictException(
                    "Lane does not belong to parking lot"
            );
        }
    }

    private void ensureLaneOperationalIfPresent(Device device) {
        if (device.getLaneId() == null) {
            return;
        }

        boolean exists = devicePortOut.existsNonClosedLaneById(
                device.getLaneId()
        );

        if (!exists) {
            throw new ConflictException(
                    "Cannot activate device for a closed lane"
            );
        }
    }

    private void ensureDeviceCodeNotExists(String deviceCode) {
        if (devicePortOut.existsByDeviceCode(deviceCode)) {
            throw new ConflictException("Device code already exists");
        }
    }

    private void ensureDeviceCodeNotUsedByOtherDevice(
            String deviceCode,
            UUID deviceId
    ) {
        if (devicePortOut.existsByDeviceCodeAndDeviceIdNot(deviceCode, deviceId)) {
            throw new ConflictException("Device code already exists");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }
}