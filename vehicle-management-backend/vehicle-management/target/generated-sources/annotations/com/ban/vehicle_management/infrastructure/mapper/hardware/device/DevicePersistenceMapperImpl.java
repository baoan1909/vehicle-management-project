package com.ban.vehicle_management.infrastructure.mapper.hardware.device;

import com.ban.vehicle_management.domain.hardware.device.model.Device;
import com.ban.vehicle_management.infrastructure.persistence.hardware.device.DeviceEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class DevicePersistenceMapperImpl implements DevicePersistenceMapper {

    @Override
    public DeviceEntity toEntity(Device domain) {
        if ( domain == null ) {
            return null;
        }

        DeviceEntity deviceEntity = new DeviceEntity();

        deviceEntity.setCreatedAt( domain.getCreatedAt() );
        deviceEntity.setCreatedBy( domain.getCreatedBy() );
        deviceEntity.setUpdatedAt( domain.getUpdatedAt() );
        deviceEntity.setUpdatedBy( domain.getUpdatedBy() );
        deviceEntity.setDeviceId( domain.getDeviceId() );
        deviceEntity.setParkingLotId( domain.getParkingLotId() );
        deviceEntity.setLaneId( domain.getLaneId() );
        deviceEntity.setDeviceCode( domain.getDeviceCode() );
        deviceEntity.setDeviceType( domain.getDeviceType() );
        deviceEntity.setName( domain.getName() );
        deviceEntity.setIpAddress( domain.getIpAddress() );
        deviceEntity.setStatus( domain.getStatus() );
        deviceEntity.setLastHeartbeatAt( domain.getLastHeartbeatAt() );
        Map<String, Object> map = domain.getConfig();
        if ( map != null ) {
            deviceEntity.setConfig( new LinkedHashMap<String, Object>( map ) );
        }

        return deviceEntity;
    }

    @Override
    public Device toDomain(DeviceEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Device device = new Device();

        device.setCreatedAt( entity.getCreatedAt() );
        device.setCreatedBy( entity.getCreatedBy() );
        device.setUpdatedAt( entity.getUpdatedAt() );
        device.setUpdatedBy( entity.getUpdatedBy() );
        device.setDeviceId( entity.getDeviceId() );
        device.setParkingLotId( entity.getParkingLotId() );
        device.setLaneId( entity.getLaneId() );
        device.setDeviceCode( entity.getDeviceCode() );
        device.setDeviceType( entity.getDeviceType() );
        device.setName( entity.getName() );
        device.setIpAddress( entity.getIpAddress() );
        device.setStatus( entity.getStatus() );
        device.setLastHeartbeatAt( entity.getLastHeartbeatAt() );
        Map<String, Object> map = entity.getConfig();
        if ( map != null ) {
            device.setConfig( new LinkedHashMap<String, Object>( map ) );
        }

        return device;
    }
}
