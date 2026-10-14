package com.ban.vehicle_management.application.parking.parkingsummary.port.in;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import java.util.UUID;

public interface PublicSpaceAvailabilityPortIn {

    PublicAvailabilityResult getPublicAvailability(UUID parkingLotId);
}
