package com.ban.vehicle_management.infrastructure.mapper.hardware;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DevicePersistenceMapper {

    DeviceEntity toEntity(Device domain);

    Device toDomain(DeviceEntity entity);
}


