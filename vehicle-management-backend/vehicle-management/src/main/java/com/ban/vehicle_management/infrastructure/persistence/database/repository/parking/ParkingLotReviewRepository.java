package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.ParkingLotReviewEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingLotReviewRepository extends JpaRepository<ParkingLotReviewEntity, UUID> {
    Optional<ParkingLotReviewEntity> findByCustomerIdAndParkingLotId(UUID customerId, UUID parkingLotId);

    @EntityGraph(attributePaths = {"customer.userProfile", "parkingLot"})
    @Query("""
            select review from ParkingLotReviewEntity review
            where review.parkingLotId = :parkingLotId
              and (:rating is null or review.rating = :rating)
            order by review.createdAt desc, review.reviewId desc
            """)
    Page<ParkingLotReviewEntity> findPublic(
            @Param("parkingLotId") UUID parkingLotId,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer.userProfile", "parkingLot"})
    @Query("""
            select review from ParkingLotReviewEntity review
            where (:unrestricted = true or review.parkingLotId in :parkingLotIds)
              and (:requestedParkingLotId is null or review.parkingLotId = :requestedParkingLotId)
              and (:rating is null or review.rating = :rating)
              and (:hasFrom = false or review.createdAt >= :from)
              and (:hasTo = false or review.createdAt < :to)
            order by review.createdAt desc, review.reviewId desc
            """)
    Page<ParkingLotReviewEntity> findInternal(
            @Param("unrestricted") boolean unrestricted,
            @Param("parkingLotIds") Set<UUID> parkingLotIds,
            @Param("requestedParkingLotId") UUID requestedParkingLotId,
            @Param("rating") Integer rating,
            @Param("hasFrom") boolean hasFrom,
            @Param("from") Instant from,
            @Param("hasTo") boolean hasTo,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
            select avg(review.rating) as averageRating, count(review) as reviewCount
            from ParkingLotReviewEntity review where review.parkingLotId = :parkingLotId
            """)
    AggregateProjection aggregate(@Param("parkingLotId") UUID parkingLotId);

    @Query("""
            select review.rating as rating, count(review) as reviewCount
            from ParkingLotReviewEntity review where review.parkingLotId = :parkingLotId
            group by review.rating
            """)
    List<RatingCountProjection> distribution(@Param("parkingLotId") UUID parkingLotId);

    @Query("""
            select review.parkingLotId as parkingLotId, avg(review.rating) as averageRating,
                   count(review) as reviewCount
            from ParkingLotReviewEntity review where review.parkingLotId in :parkingLotIds
            group by review.parkingLotId
            """)
    List<LotAggregateProjection> aggregateByParkingLotIds(@Param("parkingLotIds") Set<UUID> parkingLotIds);

    interface AggregateProjection {
        Double getAverageRating();
        long getReviewCount();
    }

    interface RatingCountProjection {
        short getRating();
        long getReviewCount();
    }

    interface LotAggregateProjection {
        UUID getParkingLotId();
        Double getAverageRating();
        long getReviewCount();
    }
}
