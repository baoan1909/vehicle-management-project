package com.ban.vehicle_management.application.people.customerfavoriteparkinglot.port.in;

import com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model.CustomerFavoriteParkingLot;
import java.util.Set;
import java.util.UUID;

public interface CustomerFavoriteParkingLotPortIn {

    Set<UUID> getMyFavoriteParkingLotIds();

    CustomerFavoriteParkingLot setMyFavoriteParkingLot(UUID parkingLotId, boolean favorite);
}

