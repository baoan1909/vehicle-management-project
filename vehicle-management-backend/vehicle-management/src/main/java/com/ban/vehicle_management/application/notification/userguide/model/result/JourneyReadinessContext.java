package com.ban.vehicle_management.application.notification.userguide.model.result;

import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult.ParkingLotReadiness;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import java.util.Set;

public record JourneyReadinessContext(
        Set<String> permissions,
        ParkingLot parkingLot,
        ParkingLotReadiness readiness,
        String operationBlockReason
) {
    public boolean has(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasAny(String... required) {
        if (permissions == null) return false;
        for (String permission : required) if (permissions.contains(permission)) return true;
        return false;
    }

    public boolean hasAll(String... required) {
        if (permissions == null) return false;
        for (String permission : required) if (!permissions.contains(permission)) return false;
        return true;
    }
}
