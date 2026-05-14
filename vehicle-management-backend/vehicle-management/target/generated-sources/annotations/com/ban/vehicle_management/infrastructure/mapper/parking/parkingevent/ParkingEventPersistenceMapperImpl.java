package com.ban.vehicle_management.infrastructure.mapper.parking.parkingevent;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.infrastructure.persistence.parking.parkingevent.ParkingEventEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class ParkingEventPersistenceMapperImpl implements ParkingEventPersistenceMapper {

    @Override
    public ParkingEventEntity toEntity(ParkingEvent domain) {
        if ( domain == null ) {
            return null;
        }

        ParkingEventEntity parkingEventEntity = new ParkingEventEntity();

        parkingEventEntity.setCreatedAt( domain.getCreatedAt() );
        parkingEventEntity.setCreatedBy( domain.getCreatedBy() );
        parkingEventEntity.setUpdatedAt( domain.getUpdatedAt() );
        parkingEventEntity.setUpdatedBy( domain.getUpdatedBy() );
        parkingEventEntity.setParkingEventId( domain.getParkingEventId() );
        parkingEventEntity.setParkingSessionId( domain.getParkingSessionId() );
        parkingEventEntity.setLaneId( domain.getLaneId() );
        parkingEventEntity.setEventType( domain.getEventType() );
        parkingEventEntity.setEventTime( domain.getEventTime() );
        parkingEventEntity.setLicensePlateDetected( domain.getLicensePlateDetected() );
        parkingEventEntity.setImagePath( domain.getImagePath() );
        parkingEventEntity.setActorAccountId( domain.getActorAccountId() );
        parkingEventEntity.setNote( domain.getNote() );

        return parkingEventEntity;
    }

    @Override
    public ParkingEvent toDomain(ParkingEventEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ParkingEvent parkingEvent = new ParkingEvent();

        parkingEvent.setCreatedAt( entity.getCreatedAt() );
        parkingEvent.setCreatedBy( entity.getCreatedBy() );
        parkingEvent.setUpdatedAt( entity.getUpdatedAt() );
        parkingEvent.setUpdatedBy( entity.getUpdatedBy() );
        parkingEvent.setParkingEventId( entity.getParkingEventId() );
        parkingEvent.setParkingSessionId( entity.getParkingSessionId() );
        parkingEvent.setLaneId( entity.getLaneId() );
        parkingEvent.setEventType( entity.getEventType() );
        parkingEvent.setEventTime( entity.getEventTime() );
        parkingEvent.setLicensePlateDetected( entity.getLicensePlateDetected() );
        parkingEvent.setImagePath( entity.getImagePath() );
        parkingEvent.setActorAccountId( entity.getActorAccountId() );
        parkingEvent.setNote( entity.getNote() );

        return parkingEvent;
    }
}
