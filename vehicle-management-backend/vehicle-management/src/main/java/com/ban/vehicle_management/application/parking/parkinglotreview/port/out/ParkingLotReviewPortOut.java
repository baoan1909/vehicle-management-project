package com.ban.vehicle_management.application.parking.parkinglotreview.port.out;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewAggregate;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewEligibilityEvidence;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewListItem;
import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParkingLotReviewPortOut {
    void lockParkingLot(UUID parkingLotId);
    ParkingLotReview save(ParkingLotReview review);
    Optional<ParkingLotReview> findByCustomerAndParkingLot(UUID customerId, UUID parkingLotId);
    Optional<ReviewEligibilityEvidence> findLatestEligibilityEvidence(UUID customerId, UUID parkingLotId);
    ReviewAggregate getAggregate(UUID parkingLotId);
    Map<UUID, ReviewAggregate> getAggregates(Set<UUID> parkingLotIds);
    Page<ReviewListItem> findPublic(UUID parkingLotId, Integer rating, Pageable pageable);
    Page<ReviewListItem> findInternal(Set<UUID> parkingLotIds, UUID requestedParkingLotId,
                                     Integer rating, Instant from, Instant to, Pageable pageable);
}
