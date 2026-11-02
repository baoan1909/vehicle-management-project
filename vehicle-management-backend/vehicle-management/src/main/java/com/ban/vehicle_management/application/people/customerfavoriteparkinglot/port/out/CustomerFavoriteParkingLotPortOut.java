package com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.out;

import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerFavoriteParkingLotPortOut {

    Optional<CustomerFavoriteParkingLot> findByCustomerIdAndParkingLotId(UUID customerId, UUID parkingLotId);

    Set<UUID> findFavoriteParkingLotIdsByCustomerId(UUID customerId);

    CustomerFavoriteParkingLot save(CustomerFavoriteParkingLot favoriteParkingLot);
}

