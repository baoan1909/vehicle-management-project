package com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ParkingLotReviewPageResponse(
        Summary summary,
        List<ParkingLotReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public record Summary(BigDecimal averageRating, long reviewCount, Map<Integer, Long> distribution) {
    }
}
