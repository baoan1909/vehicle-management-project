package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.math.BigDecimal;
import com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme;

public record UpdateParkingLotRequest(
        String name,
        AddressInputScheme addressInputScheme,
        String addressDisplay,
        String currentWardCode,
        String legacyWardCode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer totalCapacity
) {
}
