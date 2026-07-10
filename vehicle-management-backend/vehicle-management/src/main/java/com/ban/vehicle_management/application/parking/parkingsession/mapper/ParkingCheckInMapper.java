package com.ban.vehicle_management.application.parking.parkingsession.mapper;

import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.time.Instant;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingCheckInMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "parkingSessionId", source = "parkingSessionId")
    @Mapping(target = "cardId", source = "card.cardId")
    @Mapping(target = "customerId", source = "subscription.customerId")
    @Mapping(target = "customerVehicleId", source = "customerVehicle.customerVehicleId")
    @Mapping(target = "vehicleTypeId", source = "vehicleTypeId")
    @Mapping(target = "zoneId", source = "zone.zoneId")
    @Mapping(target = "licensePlateIn", source = "licensePlate")
    @Mapping(target = "checkInTime", source = "checkInTime")
    ParkingSession toOpenSession(
            UUID parkingSessionId,
            Card card,
            Subscription subscription,
            CustomerVehicle customerVehicle,
            Zone zone,
            UUID vehicleTypeId,
            String licensePlate,
            Instant checkInTime
    );

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "parkingEventId", source = "parkingEventId")
    @Mapping(target = "parkingSessionId", source = "parkingSessionId")
    @Mapping(target = "laneId", source = "laneId")
    @Mapping(target = "eventType", expression = "java(checkInEventType())")
    @Mapping(target = "eventTime", source = "eventTime")
    @Mapping(target = "licensePlateDetected", source = "licensePlate")
    @Mapping(target = "licensePlateImagePath", source = "licensePlateImagePath")
    @Mapping(target = "personImagePath", source = "personImagePath")
    @Mapping(target = "actorAccountId", source = "actorAccountId")
    @Mapping(target = "note", source = "note")
    ParkingEvent toCheckInEvent(
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

    default ParkingEventType checkInEventType() {
        return ParkingEventType.CHECK_IN;
    }
}
