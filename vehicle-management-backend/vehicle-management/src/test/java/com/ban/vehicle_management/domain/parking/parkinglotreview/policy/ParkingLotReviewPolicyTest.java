package com.ban.vehicle_management.domain.parking.parkinglotreview.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class ParkingLotReviewPolicyTest {
    @Test
    void acceptsRatingBoundaryAndNormalizesComment() {
        assertEquals(1, ParkingLotReviewPolicy.validateRating(1));
        assertEquals(5, ParkingLotReviewPolicy.validateRating(5));
        assertEquals("Dịch vụ tốt", ParkingLotReviewPolicy.normalizeComment("  Dịch vụ tốt  "));
        assertNull(ParkingLotReviewPolicy.normalizeComment("   "));
    }

    @Test
    void rejectsInvalidRatingAndUnsafeComment() {
        assertThrows(BadRequestException.class, () -> ParkingLotReviewPolicy.validateRating(0));
        assertThrows(BadRequestException.class, () -> ParkingLotReviewPolicy.validateRating(6));
        assertThrows(BadRequestException.class, () -> ParkingLotReviewPolicy.normalizeComment("<script>"));
        assertThrows(BadRequestException.class,
                () -> ParkingLotReviewPolicy.normalizeComment("a".repeat(2001)));
    }
}
