package com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFavoriteParkingLotResponse {

    private UUID parkingLotId;
    private boolean favorite;
}

