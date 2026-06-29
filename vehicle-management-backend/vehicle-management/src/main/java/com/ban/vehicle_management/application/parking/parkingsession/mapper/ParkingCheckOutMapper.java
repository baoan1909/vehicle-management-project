package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingCheckOutMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "parkingEventId", source = "parkingEventId")
    @Mapping(target = "parkingSessionId", source = "parkingSessionId")
    @Mapping(target = "laneId", source = "laneId")
    @Mapping(target = "eventType", expression = "java(checkOutEventType())")
    @Mapping(target = "eventTime", source = "eventTime")
    @Mapping(target = "licensePlateDetected", source = "licensePlate")
    @Mapping(target = "licensePlateImagePath", source = "licensePlateImagePath")
    @Mapping(target = "personImagePath", source = "personImagePath")
    @Mapping(target = "actorAccountId", source = "actorAccountId")
    @Mapping(target = "note", source = "note")
    ParkingEvent toCheckOutEvent(
            UUID parkingEventId,
            UUID parkingSessionId,
            UUID laneId,
            String licensePlate,
            String licensePlateImagePath,
            String personImagePath,
            UUID actorAccountId,
            String note,
            Instant eventTime
    );

    default ParkingEventType checkOutEventType() {
        return ParkingEventType.CHECK_OUT;
    }
}
