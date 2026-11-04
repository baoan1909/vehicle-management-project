package com.ban.vehicle_management.application.parking.parkinglotreview.port.in;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.MyParkingLotReviewResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewUpsertResult;
import java.util.UUID;

public interface CustomerParkingLotReviewPortIn {
    MyParkingLotReviewResult getMyReview(UUID parkingLotId);
    ReviewUpsertResult upsert(UUID parkingLotId, Integer rating, String comment);
}
