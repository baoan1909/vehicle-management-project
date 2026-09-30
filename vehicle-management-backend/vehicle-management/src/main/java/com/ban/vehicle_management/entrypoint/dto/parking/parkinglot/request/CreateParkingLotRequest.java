package com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request;

import java.math.BigDecimal;
import java.util.UUID;
import com.ban.vehicle_management.shared.enumeration.parking.AddressInputScheme;

public record CreateParkingLotRequest(
        UUID organizationId,
        String code,
        String name,
        String address,
        AddressInputScheme addressInputScheme,
        String addressDisplay,
        String currentWardCode,
        String legacyWardCode,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer totalCapacity
) {
}
