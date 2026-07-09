package com.ban.vehicle_management.application.hardware.device.mapper;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.request.CreateDeviceRequest;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.request.UpdateDeviceRequest;
import com.ban.vehicle_management.entrypoint.dto.hardware.device.response.DeviceAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceApiMapper {

    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Device toDomain(CreateDeviceRequest request);

    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Device toDomain(UpdateDeviceRequest request);

    DeviceAdminResponse toAdminResponse(Device device);

    List<DeviceAdminResponse> toAdminResponses(List<Device> devices);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(
                instant,
                DateTimeUtils.VIETNAM_ZONE
        );
    }
}