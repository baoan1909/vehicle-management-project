package com.ban.vehicle_management.domain.hardware.device.policy;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.shared.enumeration.hardware.DeviceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.UUID;

public class DevicePolicy {

    public void initializeNew(Device device) {
        validateEditableFields(device);
        device.setDeviceId(UUID.randomUUID());
        device.setStatus(DeviceStatus.ACTIVE);
    }

    public void validateForUpdate(Device device) {
        validateEditableFields(device);
    }

    public void activate(Device device) {
        device.setStatus(DeviceStatus.ACTIVE);
    }

    public void offline(Device device) {
        ensureNotRetired(device);
        device.setStatus(DeviceStatus.OFFLINE);
    }

    public void maintenance(Device device) {
        ensureNotRetired(device);
        device.setStatus(DeviceStatus.MAINTENANCE);
    }

    public void retire(Device device) {
        device.setStatus(DeviceStatus.RETIRED);
    }

    private void validateEditableFields(Device device) {
        if (device.getParkingLotId() == null) {
            throw new BadRequestException("parkingLotId must not be null");
        }

        if (device.getDeviceType() == null) {
            throw new BadRequestException("deviceType must not be null");
        }

        device.setDeviceCode(
                TextValidationUtils.normalizeCode(
                        device.getDeviceCode(),
                        "deviceCode",
                        50
                )
        );

        device.setName(
                TextValidationUtils.normalizeRequiredText(
                        device.getName(),
                        "name",
                        150
                )
        );

        device.setIpAddress(
                TextValidationUtils.normalizeNullableText(
                        device.getIpAddress(),
                        "ipAddress",
                        50
                )
        );
    }

    private void ensureNotRetired(Device device) {
        if (device.getStatus() == DeviceStatus.RETIRED) {
            throw new ConflictException("Retired device cannot change to this status");
        }
    }
}