package com.ban.vehicle_management.application.parking.parkinglotreview.model;

import java.util.List;

public record ReviewPageResult(
        ReviewAggregate summary, List<ReviewListItem> content, int page, int size,
        long totalElements, int totalPages, boolean first, boolean last
) {
}
