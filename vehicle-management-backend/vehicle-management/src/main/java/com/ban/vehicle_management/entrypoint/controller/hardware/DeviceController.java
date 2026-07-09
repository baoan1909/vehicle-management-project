package com.ban.vehicle_management.entrypoint.controller.hardware;

import com.ban.vehicle_management.application.hardware.device.mapper.DeviceApiMapper;
import com.ban.vehicle_management.application.hardware.device.port.in.DevicePortIn;
import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.request.CreateDeviceRequest;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.request.DeviceFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.request.UpdateDeviceRequest;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.response.DeviceAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hardware/devices")
public class DeviceController {

    private final DevicePortIn devicePortIn;
    private final DeviceApiMapper deviceApiMapper;

    public DeviceController(
            DevicePortIn devicePortIn,
            DeviceApiMapper deviceApiMapper
    ) {
        this.devicePortIn = devicePortIn;
        this.deviceApiMapper = deviceApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> create(
            @RequestBody CreateDeviceRequest request
    ) {
        Device created = devicePortIn.createDevice(
                deviceApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok(
                        "Device created successfully",
                        deviceApiMapper.toAdminResponse(created)
                )
        );
    }

    @GetMapping("/{deviceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_READ_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> getById(
            @PathVariable UUID deviceId
    ) {
        Device result = devicePortIn.getDeviceById(deviceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched device successfully",
                deviceApiMapper.toAdminResponse(result)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<DeviceAdminResponse>>> getAll(
            @ModelAttribute DeviceFilterRequest request
    ) {
        List<Device> results = devicePortIn.getDevices(
                request.parkingLotId(),
                request.laneId(),
                request.deviceType(),
                request.status(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched devices successfully",
                deviceApiMapper.toAdminResponses(results)
        ));
    }

    @PutMapping("/{deviceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> update(
            @PathVariable UUID deviceId,
            @RequestBody UpdateDeviceRequest request
    ) {
        Device updated = devicePortIn.updateDevice(
                deviceId,
                deviceApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Device updated successfully",
                deviceApiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping("/{deviceId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_STATUS_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> activate(
            @PathVariable UUID deviceId
    ) {
        Device activated = devicePortIn.activateDevice(deviceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Device activated successfully",
                deviceApiMapper.toAdminResponse(activated)
        ));
    }

    @PatchMapping("/{deviceId}/offline")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_STATUS_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> offline(
            @PathVariable UUID deviceId
    ) {
        Device offline = devicePortIn.markDeviceOffline(deviceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Device marked offline successfully",
                deviceApiMapper.toAdminResponse(offline)
        ));
    }

    @PatchMapping("/{deviceId}/maintenance")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_STATUS_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<DeviceAdminResponse>> maintenance(
            @PathVariable UUID deviceId
    ) {
        Device maintenance = devicePortIn.markDeviceMaintenance(deviceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Device marked maintenance successfully",
                deviceApiMapper.toAdminResponse(maintenance)
        ));
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('DEVICE_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID deviceId
    ) {
        devicePortIn.deleteDevice(deviceId);

        return ResponseEntity.ok(
                ApiResponse.ok("Device retired successfully")
        );
    }
}