package com.ban.vehicle_management.entrypoint.dto.parking.parkingsummary.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PublicAvailabilityResponse {
    private UUID parkingLotId;
    private String parkingLotName;
    private String parkingLotStatus;
    private long total;
    private long visitorAvailable;
    private long subscriptionAvailable;
    private boolean hasAvailability;
    private Instant snapshotAt;
    private List<VehicleTypeAvailabilityResponse> breakdown;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VehicleTypeAvailabilityResponse {
        private UUID vehicleTypeId;
        private long total;
        private long visitorAvailable;
        private long subscriptionAvailable;
    }
}
