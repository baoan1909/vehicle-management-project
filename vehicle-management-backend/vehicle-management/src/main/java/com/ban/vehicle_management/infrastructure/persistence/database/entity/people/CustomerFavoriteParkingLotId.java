package com.ban.vehicle_management.infrastructure.persistence.database.entity.people;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFavoriteParkingLotId implements Serializable {

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "parking_lot_id", nullable = false)
    private UUID parkingLotId;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CustomerFavoriteParkingLotId that)) {
            return false;
        }
        return Objects.equals(customerId, that.customerId)
                && Objects.equals(parkingLotId, that.parkingLotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, parkingLotId);
    }
}

