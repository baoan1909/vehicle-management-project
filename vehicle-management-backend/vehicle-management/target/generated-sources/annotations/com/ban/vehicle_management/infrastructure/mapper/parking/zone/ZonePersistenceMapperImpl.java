package com.ban.vehicle_management.infrastructure.mapper.parking.zone;

import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.infrastructure.persistence.parking.zone.ZoneEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ZonePersistenceMapperImpl implements ZonePersistenceMapper {

    @Override
    public ZoneEntity toEntity(Zone domain) {
        if ( domain == null ) {
            return null;
        }

        ZoneEntity zoneEntity = new ZoneEntity();

        zoneEntity.setCreatedAt( domain.getCreatedAt() );
        zoneEntity.setCreatedBy( domain.getCreatedBy() );
        zoneEntity.setUpdatedAt( domain.getUpdatedAt() );
        zoneEntity.setUpdatedBy( domain.getUpdatedBy() );
        zoneEntity.setZoneId( domain.getZoneId() );
        zoneEntity.setParkingLotId( domain.getParkingLotId() );
        zoneEntity.setCode( domain.getCode() );
        zoneEntity.setName( domain.getName() );
        zoneEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        zoneEntity.setCapacity( domain.getCapacity() );

        return zoneEntity;
    }

    @Override
    public Zone toDomain(ZoneEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Zone zone = new Zone();

        zone.setCreatedAt( entity.getCreatedAt() );
        zone.setCreatedBy( entity.getCreatedBy() );
        zone.setUpdatedAt( entity.getUpdatedAt() );
        zone.setUpdatedBy( entity.getUpdatedBy() );
        zone.setZoneId( entity.getZoneId() );
        zone.setParkingLotId( entity.getParkingLotId() );
        zone.setCode( entity.getCode() );
        zone.setName( entity.getName() );
        zone.setVehicleTypeId( entity.getVehicleTypeId() );
        zone.setCapacity( entity.getCapacity() );

        return zone;
    }
}
