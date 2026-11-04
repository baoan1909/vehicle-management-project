package com.ban.vehicle_management.application.parking.parkinglotreview.usecase;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewAggregate;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.ManageParkingLotReviewPortIn;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.out.ParkingLotReviewPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageParkingLotReviewUseCaseImpl implements ManageParkingLotReviewPortIn {
    public static final String READ_PERMISSION = "PARKING_LOT_REVIEW_READ_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final OrganizationAccessGuard accessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotReviewPortOut reviewPortOut;

    public ManageParkingLotReviewUseCaseImpl(CurrentAccountPortIn currentAccountPortIn,
                                              OrganizationAccessGuard accessGuard,
                                              ParkingLotPortOut parkingLotPortOut,
                                              ParkingLotReviewPortOut reviewPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.accessGuard = accessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.reviewPortOut = reviewPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewPageResult getReviews(UUID parkingLotId, Integer rating, Instant from, Instant to,
                                       int page, int size) {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
        PublicParkingLotReviewUseCaseImpl.validateFilter(rating, page, size);
        if (from != null && to != null && !from.isBefore(to)) {
            throw new BadRequestException("from must be before to");
        }
        Set<UUID> accessibleLots = accessGuard.resolveAccessibleParkingLotIds(parkingLotPortOut);
        if (parkingLotId != null && accessibleLots != null && !accessibleLots.contains(parkingLotId)) {
            throw new AccessDeniedException("Current account cannot access this parking lot");
        }
        var result = reviewPortOut.findInternal(accessibleLots, parkingLotId, rating, from, to,
                PageRequest.of(page, size));
        ReviewAggregate summary = parkingLotId == null ? ReviewAggregate.empty()
                : reviewPortOut.getAggregate(parkingLotId);
        return new ReviewPageResult(summary, result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
