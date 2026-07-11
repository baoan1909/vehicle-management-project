package com.ban.vehicle_management.infrastructure.mapper.parking;

import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingEventEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ParkingSessionManagementPersistenceMapper {

    List<ParkingSessionManagementResult> toResults(List<ParkingSessionEntity> entities);

    @Mapping(target = "cardNumber", source = "card.cardNumber")
    @Mapping(target = "cardUid", source = "card.uid")
    @Mapping(target = "cardTypeCode", source = "card.cardType.code")
    @Mapping(target = "cardTypeName", source = "card.cardType.name")
    @Mapping(target = "vehicleTypeCode", source = "vehicleType.code")
    @Mapping(target = "vehicleTypeName", source = "vehicleType.name")
    @Mapping(target = "zoneCode", source = "zone.code")
    @Mapping(target = "zoneName", source = "zone.name")
    @Mapping(target = "parkingLotId", source = "zone.parkingLot.parkingLotId")
    @Mapping(target = "parkingLotCode", source = "zone.parkingLot.code")
    @Mapping(target = "parkingLotName", source = "zone.parkingLot.name")
    @Mapping(target = "events", source = "parkingEvents", qualifiedByName = "toSortedEventResults")
    ParkingSessionManagementResult toResult(ParkingSessionEntity entity);

    @Mapping(target = "laneCode", source = "lane.code")
    @Mapping(target = "laneName", source = "lane.name")
    ParkingSessionManagementResult.EventResult toEventResult(ParkingEventEntity entity);

    @Named("toSortedEventResults")
    default List<ParkingSessionManagementResult.EventResult> toSortedEventResults(Set<ParkingEventEntity> events) {
        if (events == null) {
            return null;
        }
        return events.stream()
                .sorted(Comparator.comparing(ParkingEventEntity::getEventTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toEventResult)
                .toList();
    }
}
