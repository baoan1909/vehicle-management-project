package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;

public record ReviewUpsertResult(ParkingLotReview review, boolean created) {
}
