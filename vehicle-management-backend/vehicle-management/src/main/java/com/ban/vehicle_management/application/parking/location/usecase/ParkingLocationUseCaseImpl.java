package com.ban.vehicle_management.application.parking.location.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.location.model.ParkingLocationSearchResult;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationPortIn;
import com.ban.vehicle_management.application.parking.location.port.out.ParkingLocationPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ParkingLocationUseCaseImpl implements ParkingLocationPortIn {

    private static final String PARKING_LOT_CREATE_ALL = "PARKING_LOT_CREATE_ALL";
    private static final String PARKING_LOT_UPDATE_ALL = "PARKING_LOT_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final ParkingLocationPortOut parkingLocationPortOut;

    public ParkingLocationUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            ParkingLocationPortOut parkingLocationPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.parkingLocationPortOut = parkingLocationPortOut;
    }

    @Override
    public List<ParkingLocationSearchResult> search(String query) {
        requireLocationManagementPermission();
        if (query == null || query.trim().length() < 3) {
            throw new BadRequestException("location query must contain at least 3 characters");
        }
        return parkingLocationPortOut.search(query.trim());
    }

    @Override
    public List<ParkingLocationSearchResult> reverse(BigDecimal latitude, BigDecimal longitude) {
        requireLocationManagementPermission();
        if (latitude == null || longitude == null
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("parking location coordinates are invalid");
        }
        return parkingLocationPortOut.reverse(latitude, longitude);
    }

    private void requireLocationManagementPermission() {
        if (currentAccountPortIn.hasPermission(PARKING_LOT_CREATE_ALL)
                || currentAccountPortIn.hasPermission(PARKING_LOT_UPDATE_ALL)) {
            return;
        }
        currentAccountPortIn.requirePermission(PARKING_LOT_CREATE_ALL);
    }
}
