package com.ban.vehicle_management.application.parking.customerreservation.mapper;

import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationOption;
import com.ban.vehicle_management.application.parking.customerreservation.model.ParkingPlateLookupResult;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationView;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.CustomerReservationResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ParkingPlateLookupResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ReservationOptionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerReservationApiMapper {
    CustomerReservationResponse toResponse(ReservationView value);
    ReservationOptionResponse toResponse(ReservationOption value);
    ReservationOptionResponse.SpaceOptionResponse toResponse(ReservationOption.SpaceOption value);
    ParkingPlateLookupResponse toResponse(ParkingPlateLookupResult value);
    ParkingPlateLookupResponse.Session toResponse(ParkingPlateLookupResult.Session value);
    ParkingPlateLookupResponse.Reservation toResponse(ParkingPlateLookupResult.Reservation value);
}
