package com.ban.vehicle_management.application.parking.parkinglotreview.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.MyParkingLotReviewResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.model.ReviewUpsertResult;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.CustomerParkingLotReviewPortIn;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.out.ParkingLotReviewPortOut;
import com.ban.vehicle_management.application.people.customer.authorization.CustomerIdentityResolver;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.parking.parkinglotreview.model.ParkingLotReview;
import com.ban.vehicle_management.domain.parking.parkinglotreview.policy.ParkingLotReviewPolicy;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerParkingLotReviewUseCaseImpl implements CustomerParkingLotReviewPortIn {
    public static final String CREATE_PERMISSION = "PARKING_LOT_REVIEW_CREATE_OWN";
    public static final String UPDATE_PERMISSION = "PARKING_LOT_REVIEW_UPDATE_OWN";

    private final CurrentAccountPortIn currentAccountPortIn;
    private final CustomerIdentityResolver customerIdentityResolver;
    private final CustomerPortOut customerPortOut;
    private final ParkingLotPortOut parkingLotPortOut;
    private final ParkingLotReviewPortOut reviewPortOut;
    private final AuditLogPortOut auditLogPortOut;

    public CustomerParkingLotReviewUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            CustomerIdentityResolver customerIdentityResolver,
            CustomerPortOut customerPortOut,
            ParkingLotPortOut parkingLotPortOut,
            ParkingLotReviewPortOut reviewPortOut,
            AuditLogPortOut auditLogPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.customerIdentityResolver = customerIdentityResolver;
        this.customerPortOut = customerPortOut;
        this.parkingLotPortOut = parkingLotPortOut;
        this.reviewPortOut = reviewPortOut;
        this.auditLogPortOut = auditLogPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public MyParkingLotReviewResult getMyReview(UUID parkingLotId) {
        var lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        var access = currentAccountPortIn.getCurrentAccountOrThrow();
        UUID customerId = customerPortOut.findCustomerIdByAccountId(access.accountId()).orElse(null);
        ParkingLotReview currentReview = customerId == null ? null
                : reviewPortOut.findByCustomerAndParkingLot(customerId, parkingLotId).orElse(null);
        if (!CustomerStatus.ACTIVE.equals(access.customerStatus())) {
            return result(false, "CUSTOMER_NOT_ACTIVE", lot.getParkingLotId(), lot.getName(), currentReview);
        }
        if (!CustomerApprovalStatus.APPROVED.equals(access.customerApprovalStatus())) {
            return result(false, "CUSTOMER_NOT_APPROVED", lot.getParkingLotId(), lot.getName(), currentReview);
        }
        if (currentReview != null) {
            return result(true, "ELIGIBLE_EXISTING_REVIEW", lot.getParkingLotId(), lot.getName(), currentReview);
        }
        boolean eligible = customerId != null
                && reviewPortOut.findLatestEligibilityEvidence(customerId, parkingLotId).isPresent();
        return result(eligible, eligible ? "ELIGIBLE_TRANSACTION" : "NO_ELIGIBLE_TRANSACTION",
                lot.getParkingLotId(), lot.getName(), null);
    }

    @Override
    @Transactional
    public ReviewUpsertResult upsert(UUID parkingLotId, Integer rating, String comment) {
        short normalizedRating = ParkingLotReviewPolicy.validateRating(rating);
        String normalizedComment = ParkingLotReviewPolicy.normalizeComment(comment);
        UUID customerId = customerIdentityResolver.resolveCurrentApprovedCustomerId();
        var lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));

        // Serializing on the lot closes the first-review race before the unique constraint is reached.
        reviewPortOut.lockParkingLot(parkingLotId);
        ParkingLotReview review = reviewPortOut.findByCustomerAndParkingLot(customerId, parkingLotId).orElse(null);
        boolean created = review == null;
        Integer oldRating = null;
        if (created) {
            currentAccountPortIn.requirePermission(CREATE_PERMISSION);
            var evidence = reviewPortOut.findLatestEligibilityEvidence(customerId, parkingLotId)
                    .orElseThrow(() -> new BadRequestException("NO_ELIGIBLE_TRANSACTION"));
            review = new ParkingLotReview();
            review.setReviewId(UUID.randomUUID());
            review.setOrganizationId(lot.getOrganizationId());
            review.setParkingLotId(parkingLotId);
            review.setCustomerId(customerId);
            review.setSourceParkingSessionId(evidence.parkingSessionId());
            review.setSourcePaymentId(evidence.paymentId());
        } else {
            currentAccountPortIn.requirePermission(UPDATE_PERMISSION);
            oldRating = (int) review.getRating();
        }
        review.setRating(normalizedRating);
        review.setComment(normalizedComment);
        ParkingLotReview saved = reviewPortOut.save(review);
        audit(saved, created ? "CREATE_PARKING_LOT_REVIEW" : "UPDATE_PARKING_LOT_REVIEW", oldRating);
        return new ReviewUpsertResult(saved, created);
    }

    private MyParkingLotReviewResult result(boolean eligible, String reasonCode, UUID lotId, String lotName,
                                             ParkingLotReview review) {
        return new MyParkingLotReviewResult(eligible, reasonCode, lotId, lotName, review);
    }

    private void audit(ParkingLotReview review, String action, Integer oldRating) {
        AuditLog log = new AuditLog();
        log.setAuditLogId(UUID.randomUUID());
        log.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        log.setAction(action);
        log.setTargetSchema("parking");
        log.setTargetTable("parking_lot_reviews");
        log.setTargetId(review.getReviewId());
        log.setOldData(oldRating == null ? Map.of() : Map.of("rating", oldRating));
        log.setNewData(Map.of("parkingLotId", review.getParkingLotId(), "rating", (int) review.getRating()));
        auditLogPortOut.save(log);
    }
}
