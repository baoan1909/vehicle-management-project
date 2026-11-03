package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import java.math.BigDecimal;
import java.util.Map;

public record ReviewAggregate(BigDecimal averageRating, long reviewCount, Map<Integer, Long> distribution) {
    public static ReviewAggregate empty() {
        return new ReviewAggregate(null, 0, Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
    }
}
