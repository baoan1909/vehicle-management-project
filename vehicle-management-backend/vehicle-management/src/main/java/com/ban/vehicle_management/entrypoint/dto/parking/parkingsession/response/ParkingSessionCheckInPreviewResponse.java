package com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response;

import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParkingSessionCheckInPreviewResponse {
    private UUID parkingLotId;
    private UUID zoneId;
    private TrackingMode trackingMode;
    private String customerType;
    private long physicalAvailable;
    private long visitorAvailable;
    private boolean selectionRequired;
    private UUID recommendedParkingSpaceId;
    private List<EligibleSpaceResponse> eligibleSpaces;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EligibleSpaceResponse {
        private UUID parkingSpaceId;
        private String code;
        private ParkingSpaceStatus status;
    }
}
