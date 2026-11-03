package com.ban.vehicle_management.infrastructure.persistence.adapter.parking;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewAggregate;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewEligibilityEvidence;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewListItem;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.out.ParkingLotReviewPortOut;
import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import com.ban.vehicle_management.infrastructure.mapper.parking.ParkingLotReviewPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotReviewEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingSessionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingLotReviewRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotReviewPersistenceAdapter implements ParkingLotReviewPortOut {
    private static final Set<UUID> DUMMY_SCOPE = Set.of(new UUID(0, 0));

    private final ParkingLotReviewRepository reviewRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final ParkingLotReviewPersistenceMapper mapper;

    public ParkingLotReviewPersistenceAdapter(
            ParkingLotReviewRepository reviewRepository,
            ParkingLotRepository parkingLotRepository,
            ParkingSessionRepository parkingSessionRepository,
            PaymentRepository paymentRepository,
            ParkingLotReviewPersistenceMapper mapper
    ) {
        this.reviewRepository = reviewRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.mapper = mapper;
    }

    @Override
    public void lockParkingLot(UUID parkingLotId) {
        parkingLotRepository.findByIdForUpdate(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
    }

    @Override
    public ParkingLotReview save(ParkingLotReview review) {
        return mapper.toDomain(reviewRepository.saveAndFlush(mapper.toEntity(review)));
    }

    @Override
    public Optional<ParkingLotReview> findByCustomerAndParkingLot(UUID customerId, UUID parkingLotId) {
        return reviewRepository.findByCustomerIdAndParkingLotId(customerId, parkingLotId).map(mapper::toDomain);
    }

    @Override
    public Optional<ReviewEligibilityEvidence> findLatestEligibilityEvidence(UUID customerId, UUID parkingLotId) {
        Optional<ReviewEligibilityEvidence> sessionEvidence = parkingSessionRepository
                .findFirstByCustomerIdAndParkingLotIdAndStatusOrderByCheckOutTimeDesc(
                        customerId, parkingLotId, ParkingSessionStatus.CLOSED)
                .map(session -> ReviewEligibilityEvidence.session(
                        session.getParkingSessionId(), session.getCheckOutTime()));
        Optional<ReviewEligibilityEvidence> paymentEvidence = paymentRepository
                .findLatestReviewEligiblePayment(customerId, parkingLotId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(payment -> ReviewEligibilityEvidence.payment(
                        payment.getPaymentId(), payment.getPaidAt() == null
                                ? payment.getCreatedAt() : payment.getPaidAt()));
        if (sessionEvidence.isEmpty()) {
            return paymentEvidence;
        }
        if (paymentEvidence.isEmpty()) {
            return sessionEvidence;
        }
        Instant sessionAt = sessionEvidence.get().occurredAt();
        Instant paymentAt = paymentEvidence.get().occurredAt();
        return paymentAt != null && (sessionAt == null || paymentAt.isAfter(sessionAt))
                ? paymentEvidence : sessionEvidence;
    }

    @Override
    public ReviewAggregate getAggregate(UUID parkingLotId) {
        ParkingLotReviewRepository.AggregateProjection projection = reviewRepository.aggregate(parkingLotId);
        Map<Integer, Long> distribution = emptyDistribution();
        reviewRepository.distribution(parkingLotId)
                .forEach(row -> distribution.put((int) row.getRating(), row.getReviewCount()));
        return new ReviewAggregate(round(projection == null ? null : projection.getAverageRating()),
                projection == null ? 0 : projection.getReviewCount(), Map.copyOf(distribution));
    }

    @Override
    public Map<UUID, ReviewAggregate> getAggregates(Set<UUID> parkingLotIds) {
        if (parkingLotIds == null || parkingLotIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ReviewAggregate> result = new HashMap<>();
        reviewRepository.aggregateByParkingLotIds(parkingLotIds).forEach(row -> result.put(
                row.getParkingLotId(),
                new ReviewAggregate(round(row.getAverageRating()), row.getReviewCount(), Map.of())
        ));
        return Map.copyOf(result);
    }

    @Override
    public Page<ReviewListItem> findPublic(UUID parkingLotId, Integer rating, Pageable pageable) {
        return reviewRepository.findPublic(parkingLotId, rating, pageable).map(this::toListItem);
    }

    @Override
    public Page<ReviewListItem> findInternal(Set<UUID> parkingLotIds, UUID requestedParkingLotId,
                                             Integer rating, Instant from, Instant to, Pageable pageable) {
        if (parkingLotIds != null && parkingLotIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        // PostgreSQL cannot infer the type of a null Instant used only in an
        // `is null` JPQL predicate. Keep every temporal bind typed and control
        // optional filters with explicit boolean flags instead.
        Instant boundFrom = from == null ? Instant.EPOCH : from;
        Instant boundTo = to == null ? Instant.EPOCH : to;
        return reviewRepository.findInternal(parkingLotIds == null, parkingLotIds == null ? DUMMY_SCOPE : parkingLotIds,
                requestedParkingLotId, rating, from != null, boundFrom, to != null, boundTo, pageable)
                .map(this::toListItem);
    }

    private ReviewListItem toListItem(ParkingLotReviewEntity entity) {
        String fullName = entity.getCustomer() == null || entity.getCustomer().getUserProfile() == null
                ? null : entity.getCustomer().getUserProfile().getFullName();
        return new ReviewListItem(entity.getReviewId(), entity.getParkingLotId(),
                entity.getParkingLot() == null ? null : entity.getParkingLot().getName(), entity.getCustomerId(),
                maskName(fullName),
                entity.getRating(), entity.getComment(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Khách hàng";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder masked = new StringBuilder();
        for (String part : parts) {
            if (!masked.isEmpty()) {
                masked.append(' ');
            }
            masked.append(part.charAt(0)).append("***");
        }
        return masked.toString();
    }

    private Map<Integer, Long> emptyDistribution() {
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (int rating = 1; rating <= 5; rating++) {
            result.put(rating, 0L);
        }
        return result;
    }

    private BigDecimal round(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }
}
