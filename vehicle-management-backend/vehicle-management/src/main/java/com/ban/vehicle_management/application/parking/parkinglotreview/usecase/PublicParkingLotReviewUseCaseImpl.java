package com.ban.vehicle_management.application.parking.parkinglotreview.usecase;

import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.PublicParkingLotReviewPortIn;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.out.ParkingLotReviewPortOut;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicParkingLotReviewUseCaseImpl implements PublicParkingLotReviewPortIn {
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotReviewPortOut reviewPortOut;

    public PublicParkingLotReviewUseCaseImpl(ParkingLotPortOut parkingLotPortOut,
                                             ParkingLotReviewPortOut reviewPortOut) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.reviewPortOut = reviewPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResult getReviews(UUID parkingLotId, Integer rating, int page, int size) {
        var lot = parkingLotPortOut.findById(parkingLotId)
                .filter(value -> ParkingLotStatus.ACTIVE.equals(value.getStatus()))
                .orElseThrow(() -> new NotFoundException("Active parking lot not found"));
        validateFilter(rating, page, size);
        var result = reviewPortOut.findPublic(lot.getParkingLotId(), rating, PageRequest.of(page, size));
        return new ReviewPageResult(reviewPortOut.getAggregate(parkingLotId), result.getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    static void validateFilter(Integer rating, int page, int size) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new BadRequestException("rating must be between 1 and 5");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("page must be non-negative and size must be between 1 and 100");
        }
    }
}
