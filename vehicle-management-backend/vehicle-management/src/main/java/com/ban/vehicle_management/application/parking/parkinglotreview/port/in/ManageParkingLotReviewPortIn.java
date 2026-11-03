package com.ban.vehicle_management.application.parking.parkinglotreview.port.in;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import java.time.Instant;
import java.util.UUID;

public interface ManageParkingLotReviewPortIn {
    ReviewPageResult getReviews(UUID parkingLotId, Integer rating, Instant from, Instant to, int page, int size);
}
