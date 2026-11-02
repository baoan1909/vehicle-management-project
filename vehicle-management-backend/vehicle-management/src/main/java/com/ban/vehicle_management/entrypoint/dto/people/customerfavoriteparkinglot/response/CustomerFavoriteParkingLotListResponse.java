package com.ban.vehicle_management.entrypoint.dto.people.customerfavoriteparkinglot.response;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFavoriteParkingLotListResponse {

    private Set<UUID> parkingLotIds;
}

