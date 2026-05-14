package com.ban.vehicle_management.infrastructure.mapper.parking.parkinglot;

import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkinglot.ParkingLotEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:11+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ParkingLotPersistenceMapperImpl implements ParkingLotPersistenceMapper {

    @Override
    public ParkingLotEntity toEntity(ParkingLot domain) {
        if ( domain == null ) {
            return null;
        }

        ParkingLotEntity parkingLotEntity = new ParkingLotEntity();

        parkingLotEntity.setCreatedAt( domain.getCreatedAt() );
        parkingLotEntity.setCreatedBy( domain.getCreatedBy() );
        parkingLotEntity.setUpdatedAt( domain.getUpdatedAt() );
        parkingLotEntity.setUpdatedBy( domain.getUpdatedBy() );
        parkingLotEntity.setParkingLotId( domain.getParkingLotId() );
        parkingLotEntity.setCode( domain.getCode() );
        parkingLotEntity.setName( domain.getName() );
        parkingLotEntity.setAddress( domain.getAddress() );
        parkingLotEntity.setTotalCapacity( domain.getTotalCapacity() );
        parkingLotEntity.setStatus( domain.getStatus() );

        return parkingLotEntity;
    }

    @Override
    public ParkingLot toDomain(ParkingLotEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ParkingLot parkingLot = new ParkingLot();

        parkingLot.setCreatedAt( entity.getCreatedAt() );
        parkingLot.setCreatedBy( entity.getCreatedBy() );
        parkingLot.setUpdatedAt( entity.getUpdatedAt() );
        parkingLot.setUpdatedBy( entity.getUpdatedBy() );
        parkingLot.setParkingLotId( entity.getParkingLotId() );
        parkingLot.setCode( entity.getCode() );
        parkingLot.setName( entity.getName() );
        parkingLot.setAddress( entity.getAddress() );
        parkingLot.setTotalCapacity( entity.getTotalCapacity() );
        parkingLot.setStatus( entity.getStatus() );

        return parkingLot;
    }
}
