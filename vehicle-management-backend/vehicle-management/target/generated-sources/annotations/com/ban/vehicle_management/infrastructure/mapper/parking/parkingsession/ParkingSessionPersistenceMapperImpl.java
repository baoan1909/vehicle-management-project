package com.ban.vehicle_management.infrastructure.mapper.parking.parkingsession;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingsession.ParkingSessionEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ParkingSessionPersistenceMapperImpl implements ParkingSessionPersistenceMapper {

    @Override
    public ParkingSessionEntity toEntity(ParkingSession domain) {
        if ( domain == null ) {
            return null;
        }

        ParkingSessionEntity parkingSessionEntity = new ParkingSessionEntity();

        parkingSessionEntity.setCreatedAt( domain.getCreatedAt() );
        parkingSessionEntity.setCreatedBy( domain.getCreatedBy() );
        parkingSessionEntity.setUpdatedAt( domain.getUpdatedAt() );
        parkingSessionEntity.setUpdatedBy( domain.getUpdatedBy() );
        parkingSessionEntity.setParkingSessionId( domain.getParkingSessionId() );
        parkingSessionEntity.setCardId( domain.getCardId() );
        parkingSessionEntity.setCustomerId( domain.getCustomerId() );
        parkingSessionEntity.setCustomerVehicleId( domain.getCustomerVehicleId() );
        parkingSessionEntity.setVehicleTypeId( domain.getVehicleTypeId() );
        parkingSessionEntity.setParkingSpaceId( domain.getParkingSpaceId() );
        parkingSessionEntity.setLicensePlateIn( domain.getLicensePlateIn() );
        parkingSessionEntity.setLicensePlateOut( domain.getLicensePlateOut() );
        parkingSessionEntity.setCheckInTime( domain.getCheckInTime() );
        parkingSessionEntity.setCheckOutTime( domain.getCheckOutTime() );
        parkingSessionEntity.setStatus( domain.getStatus() );
        parkingSessionEntity.setTotalPrice( domain.getTotalPrice() );
        parkingSessionEntity.setPriceRuleId( domain.getPriceRuleId() );

        return parkingSessionEntity;
    }

    @Override
    public ParkingSession toDomain(ParkingSessionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ParkingSession parkingSession = new ParkingSession();

        parkingSession.setCreatedAt( entity.getCreatedAt() );
        parkingSession.setCreatedBy( entity.getCreatedBy() );
        parkingSession.setUpdatedAt( entity.getUpdatedAt() );
        parkingSession.setUpdatedBy( entity.getUpdatedBy() );
        parkingSession.setParkingSessionId( entity.getParkingSessionId() );
        parkingSession.setCardId( entity.getCardId() );
        parkingSession.setCustomerId( entity.getCustomerId() );
        parkingSession.setCustomerVehicleId( entity.getCustomerVehicleId() );
        parkingSession.setVehicleTypeId( entity.getVehicleTypeId() );
        parkingSession.setParkingSpaceId( entity.getParkingSpaceId() );
        parkingSession.setLicensePlateIn( entity.getLicensePlateIn() );
        parkingSession.setLicensePlateOut( entity.getLicensePlateOut() );
        parkingSession.setCheckInTime( entity.getCheckInTime() );
        parkingSession.setCheckOutTime( entity.getCheckOutTime() );
        parkingSession.setStatus( entity.getStatus() );
        parkingSession.setTotalPrice( entity.getTotalPrice() );
        parkingSession.setPriceRuleId( entity.getPriceRuleId() );

        return parkingSession;
    }
}
