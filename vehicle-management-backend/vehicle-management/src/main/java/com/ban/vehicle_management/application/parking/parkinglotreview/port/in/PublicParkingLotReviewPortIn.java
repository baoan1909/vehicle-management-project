package com.ban.vehicle_management.application.parking.parkinglotreview.port.in;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import java.util.UUID;

public interface PublicParkingLotReviewPortIn {
    ReviewPageResult getReviews(UUID parkingLotId, Integer rating, int page, int size);
}
