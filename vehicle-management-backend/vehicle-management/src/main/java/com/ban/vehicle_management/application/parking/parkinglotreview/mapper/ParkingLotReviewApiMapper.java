package com.ban.vehicle_management.application.parking.parkinglotreview.mapper;

import com.ban.vehicle_management.application.parking.parkinglotreview.model.MyParkingLotReviewResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewListItem;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewPageResult;
import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.MyParkingLotReviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.ParkingLotReviewPageResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.ParkingLotReviewResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ParkingLotReviewApiMapper {
    default ParkingLotReviewPageResponse toPublicPageResponse(ReviewPageResult result) {
        return toPageResponse(result, false);
    }

    default ParkingLotReviewPageResponse toManagementPageResponse(ReviewPageResult result) {
        return toPageResponse(result, true);
    }

    private ParkingLotReviewPageResponse toPageResponse(ReviewPageResult result, boolean includeReviewerCustomerId) {
        return new ParkingLotReviewPageResponse(
                new ParkingLotReviewPageResponse.Summary(result.summary().averageRating(),
                        result.summary().reviewCount(), result.summary().distribution()),
                result.content().stream()
                        .map(item -> toResponse(item, includeReviewerCustomerId))
                        .toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                result.first(), result.last());
    }

    private ParkingLotReviewResponse toResponse(ReviewListItem item, boolean includeReviewerCustomerId) {
        return new ParkingLotReviewResponse(item.parkingLotName(), item.reviewerDisplayName(),
                includeReviewerCustomerId ? item.reviewerCustomerId() : null,
                item.rating(), item.comment(), true, item.createdAt(),
                item.updatedAt(), item.edited());
    }

    default MyParkingLotReviewResponse toMyResponse(MyParkingLotReviewResult result) {
        return new MyParkingLotReviewResponse(result.eligible(), result.reasonCode(), result.parkingLotId(),
                result.parkingLotName(), toCurrentReview(result.currentReview()));
    }

    default MyParkingLotReviewResponse.CurrentReview toCurrentReview(ParkingLotReview review) {
        if (review == null) {
            return null;
        }
        boolean edited = review.getUpdatedAt() != null && review.getCreatedAt() != null
                && review.getUpdatedAt().isAfter(review.getCreatedAt());
        return new MyParkingLotReviewResponse.CurrentReview(review.getRating(),
                review.getComment(), review.getCreatedAt(), review.getUpdatedAt(), edited);
    }
}
