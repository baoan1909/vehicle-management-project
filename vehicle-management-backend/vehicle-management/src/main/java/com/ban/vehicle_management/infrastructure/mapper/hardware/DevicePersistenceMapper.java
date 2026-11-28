package com.ban.vehicle_management.infrastructure.mapper.hardware;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.domain.hardware.device.model.DeviceLaneOption;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.hardware.DeviceEntity;
import org.mapstruct.Mapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.DeviceLaneOptionProjection;

@Mapper(componentModel = "spring")
public interface DevicePersistenceMapper {

    DeviceLaneOption toLaneOption(DeviceLaneOptionProjection projection);
    DeviceEntity toEntity(Device domain);

    Device toDomain(DeviceEntity entity);
}


