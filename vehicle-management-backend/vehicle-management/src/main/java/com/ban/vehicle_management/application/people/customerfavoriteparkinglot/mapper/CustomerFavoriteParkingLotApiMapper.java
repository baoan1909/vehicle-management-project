package com.ban.vehicle_management.application.people.customerfavoriteparkinglot.mapper;

import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response.CustomerFavoriteParkingLotListResponse;
import com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response.CustomerFavoriteParkingLotResponse;
import java.util.Set;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerFavoriteParkingLotApiMapper {

    CustomerFavoriteParkingLotResponse toResponse(CustomerFavoriteParkingLot domain);

    default CustomerFavoriteParkingLotListResponse toListResponse(Set<UUID> parkingLotIds) {
        return new CustomerFavoriteParkingLotListResponse(parkingLotIds);
    }
}

