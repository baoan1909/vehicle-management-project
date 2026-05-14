package com.ban.vehicle_management.infrastructure.mapper.parking.parkingspace;

import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingspace.ParkingSpaceEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ParkingSpacePersistenceMapperImpl implements ParkingSpacePersistenceMapper {

    @Override
    public ParkingSpaceEntity toEntity(ParkingSpace domain) {
        if ( domain == null ) {
            return null;
        }

        ParkingSpaceEntity parkingSpaceEntity = new ParkingSpaceEntity();

        parkingSpaceEntity.setCreatedAt( domain.getCreatedAt() );
        parkingSpaceEntity.setCreatedBy( domain.getCreatedBy() );
        parkingSpaceEntity.setUpdatedAt( domain.getUpdatedAt() );
        parkingSpaceEntity.setUpdatedBy( domain.getUpdatedBy() );
        parkingSpaceEntity.setParkingSpaceId( domain.getParkingSpaceId() );
        parkingSpaceEntity.setZoneId( domain.getZoneId() );
        parkingSpaceEntity.setCode( domain.getCode() );
        parkingSpaceEntity.setStatus( domain.getStatus() );

        return parkingSpaceEntity;
    }

    @Override
    public ParkingSpace toDomain(ParkingSpaceEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ParkingSpace parkingSpace = new ParkingSpace();

        parkingSpace.setCreatedAt( entity.getCreatedAt() );
        parkingSpace.setCreatedBy( entity.getCreatedBy() );
        parkingSpace.setUpdatedAt( entity.getUpdatedAt() );
        parkingSpace.setUpdatedBy( entity.getUpdatedBy() );
        parkingSpace.setParkingSpaceId( entity.getParkingSpaceId() );
        parkingSpace.setZoneId( entity.getZoneId() );
        parkingSpace.setCode( entity.getCode() );
        parkingSpace.setStatus( entity.getStatus() );

        return parkingSpace;
    }
}
