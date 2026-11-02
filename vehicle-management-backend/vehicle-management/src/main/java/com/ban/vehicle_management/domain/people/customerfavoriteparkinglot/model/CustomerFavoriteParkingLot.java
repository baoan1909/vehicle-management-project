package com.ban.vehicle_management.domain.people.customerfavoriteparkinglot.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFavoriteParkingLot extends AuditableDomainModel {

    private UUID customerId;
    private UUID parkingLotId;
    private boolean favorite;
    private Instant favoritedAt;
    private Instant unfavoritedAt;

    public static CustomerFavoriteParkingLot create(
            UUID customerId,
            UUID parkingLotId,
            Instant changedAt
    ) {
        CustomerFavoriteParkingLot favoriteParkingLot = new CustomerFavoriteParkingLot();
        favoriteParkingLot.customerId = customerId;
        favoriteParkingLot.parkingLotId = parkingLotId;
        favoriteParkingLot.favorite = true;
        favoriteParkingLot.favoritedAt = changedAt;
        return favoriteParkingLot;
    }

    public void changeFavorite(boolean nextFavorite, Instant changedAt) {
        if (favorite == nextFavorite) {
            return;
        }
        favorite = nextFavorite;
        if (nextFavorite) {
            favoritedAt = changedAt;
            unfavoritedAt = null;
        } else {
            unfavoritedAt = changedAt;
        }
    }
}

