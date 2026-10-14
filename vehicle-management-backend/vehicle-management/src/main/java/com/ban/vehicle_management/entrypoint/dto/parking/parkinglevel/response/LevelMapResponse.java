package com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response;

import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.response.ZoneAdminResponse;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LevelMapResponse {
    private ParkingLevelAdminResponse level;
    private List<ZoneAdminResponse> zones;
    private List<ParkingSpaceAdminResponse> spaces;
    private ParkingSpaceSummaryResponse summary;
}
