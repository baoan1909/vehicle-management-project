package com.ban.vehicle_management.application.parking.parkinglot.mapper;

import com.ban.vehicle_management.application.parking.parkinglot.model.result.NearbyParkingLotResult;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.CreateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.UpdateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingLotAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingLotPublicResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParkingLotApiMapper {

    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "activationRequestedAt", ignore = true)
    @Mapping(target = "activationRequestedBy", ignore = true)
    @Mapping(target = "geocodingStatus", ignore = true)
    @Mapping(target = "geocodedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLot toDomain(CreateParkingLotRequest request);

    @Mapping(target = "parkingLotId", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "activationRequestedAt", ignore = true)
    @Mapping(target = "activationRequestedBy", ignore = true)
    @Mapping(target = "geocodingStatus", ignore = true)
    @Mapping(target = "geocodedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ParkingLot toDomain(UpdateParkingLotRequest request);

    ParkingLotAdminResponse toAdminResponse(ParkingLot parkingLot);

    List<ParkingLotAdminResponse> toAdminResponses(List<ParkingLot> parkingLots);

    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "distanceKm", ignore = true)
    ParkingLotPublicResponse toPublicResponse(ParkingLot parkingLot);

    List<ParkingLotPublicResponse> toPublicResponses(List<ParkingLot> parkingLots);

    default ParkingLotPublicResponse toNearbyPublicResponse(NearbyParkingLotResult parkingLot) {
        long distanceMeters = BigDecimal.valueOf(parkingLot.distanceMeters())
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        BigDecimal distanceKm = BigDecimal.valueOf(parkingLot.distanceMeters())
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        return new ParkingLotPublicResponse(
                parkingLot.parkingLotId(),
                parkingLot.name(),
                parkingLot.addressDisplay(),
                parkingLot.latitude(),
                parkingLot.longitude(),
                distanceMeters,
                distanceKm
        );
    }

    default List<ParkingLotPublicResponse> toNearbyPublicResponses(List<NearbyParkingLotResult> parkingLots) {
        return parkingLots.stream().map(this::toNearbyPublicResponse).toList();
    }
}
