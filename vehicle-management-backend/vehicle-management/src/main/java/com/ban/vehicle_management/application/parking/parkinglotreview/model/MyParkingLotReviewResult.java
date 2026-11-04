package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import java.util.UUID;

public record MyParkingLotReviewResult(
        boolean eligible, String reasonCode, UUID parkingLotId, String parkingLotName,
        ParkingLotReview currentReview
) {
}
