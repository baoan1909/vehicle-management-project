package com.ban.vehicle_management.domain.parking.parkinglotreview.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public final class ParkingLotReviewPolicy {
    public static final int MAX_COMMENT_LENGTH = 2000;

    private ParkingLotReviewPolicy() {
    }

    public static short validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new BadRequestException("rating must be between 1 and 5");
        }
        return rating.shortValue();
    }

    public static String normalizeComment(String comment) {
        return TextValidationUtils.normalizeNullableText(comment, "comment", MAX_COMMENT_LENGTH);
    }
}
