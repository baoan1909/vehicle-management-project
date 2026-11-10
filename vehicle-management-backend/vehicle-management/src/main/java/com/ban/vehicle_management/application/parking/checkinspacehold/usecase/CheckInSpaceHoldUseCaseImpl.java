package com.ban.vehicle_management.application.parking.checkinspacehold.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.checkinspacehold.config.CheckInSpaceHoldProperties;
import com.ban.vehicle_management.application.parking.checkinspacehold.model.command.CreateCheckInSpaceHoldCommand;
import com.ban.vehicle_management.application.parking.checkinspacehold.model.result.CheckInSpaceHoldResult;
import com.ban.vehicle_management.application.parking.checkinspacehold.port.in.CheckInSpaceHoldPortIn;
import com.ban.vehicle_management.application.parking.checkinspacehold.port.out.CheckInSpaceHoldPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInPreviewCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.realtime.ParkingRealtimeAfterCommit;
import com.ban.vehicle_management.application.parking.realtime.model.ParkingRealtimeEvent;
import com.ban.vehicle_management.application.parking.realtime.port.out.ParkingRealtimePublisherPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.parking.checkinspacehold.model.CheckInSpaceHold;
import com.ban.vehicle_management.domain.parking.checkinspacehold.policy.CheckInSpaceHoldPolicy;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingLicensePlatePolicy;
import com.ban.vehicle_management.shared.enumeration.parking.CheckInSpaceHoldStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.TrackingMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckInSpaceHoldUseCaseImpl implements CheckInSpaceHoldPortIn {
    private static final int MAX_QR_PAYLOAD_LENGTH = 512;
    private static final int EXPIRY_BATCH_SIZE = 100;

    private final ParkingSessionPortIn parkingSessionPortIn;
    private final CheckInSpaceHoldPortOut holdPortOut;
    private final ParkingSpacePortOut spacePortOut;
    private final CardPortOut cardPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CheckInSpaceHoldProperties properties;
    private final CheckInSpaceHoldPolicy policy = new CheckInSpaceHoldPolicy();
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private ParkingRealtimePublisherPortOut realtimePublisher;

    @Autowired
    public void setRealtimePublisher(ParkingRealtimePublisherPortOut realtimePublisher) {
        this.realtimePublisher = realtimePublisher;
    }

    public CheckInSpaceHoldUseCaseImpl(
            ParkingSessionPortIn parkingSessionPortIn,
            CheckInSpaceHoldPortOut holdPortOut,
            ParkingSpacePortOut spacePortOut,
            CardPortOut cardPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            CheckInSpaceHoldProperties properties
    ) {
        this.parkingSessionPortIn = parkingSessionPortIn;
        this.holdPortOut = holdPortOut;
        this.spacePortOut = spacePortOut;
        this.cardPortOut = cardPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.properties = properties;
    }

    @Override
    @Transactional
    public CheckInSpaceHoldResult create(CreateCheckInSpaceHoldCommand command) {
        if (command == null || command.requestId() == null) {
            throw new BadRequestException("requestId must not be null");
        }
        UUID operatorId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        holdPortOut.lockTransactionKey("check-in-hold:request:" + command.requestId());
        var existing = holdPortOut.findByRequestId(command.requestId());
        if (existing.isPresent()) {
            ensureOwner(existing.get(), operatorId);
            return toResult(existing.get(), false);
        }

        Instant now = Instant.now();
        expireDueBatches(now);
        CheckInPreviewResult preview = parkingSessionPortIn.previewCheckIn(new CheckInPreviewCommand(
                command.cardUid(), command.laneId(), command.vehicleTypeId(), command.licensePlate(),
                command.subscriptionQrPayload(), command.reservationQrPayload()));
        if (preview.trackingMode() != TrackingMode.SPACE) {
            throw new BadRequestException("Parking-space hold is only available for SPACE zones");
        }

        String normalizedPlate = licensePlatePolicy.normalizeRequired(command.licensePlate(), "licensePlate");
        holdPortOut.lockTransactionKey(
                "check-in-hold:plate:" + preview.parkingLotId() + ":" + normalizedPlate);
        Card card = resolvePhysicalCard(command.cardUid(), command.subscriptionQrPayload());
        holdPortOut.findHeldByPlateAndLot(preview.parkingLotId(), normalizedPlate)
                .ifPresent(hold -> { throw new ConflictException("License plate already has an active check-in hold"); });
        if (card != null) {
            holdPortOut.findHeldByCardId(card.getCardId())
                    .ifPresent(hold -> { throw new ConflictException("Card already has an active check-in hold"); });
        }

        ParkingSpace space = selectSpace(preview, now);
        CheckInSpaceHold hold = new CheckInSpaceHold();
        hold.setHoldId(UUID.randomUUID());
        hold.setRequestId(command.requestId());
        hold.setParkingSpaceId(space.getParkingSpaceId());
        hold.setParkingLotId(preview.parkingLotId());
        hold.setZoneId(preview.zoneId());
        hold.setLaneId(command.laneId());
        hold.setCardId(card == null ? null : card.getCardId());
        hold.setSubscriptionId(preview.subscriptionId());
        hold.setCustomerReservationId(preview.customerReservationId());
        hold.setVehicleTypeId(preview.vehicleTypeId());
        hold.setLicensePlateNormalized(normalizedPlate);
        hold.setCredentialHash(hashCredential(command));
        hold.setCustomerType(preview.customerType());
        hold.setOperatorAccountId(operatorId);
        policy.initialize(hold, now, now.plus(properties.getDuration()));
        CheckInSpaceHold saved = holdPortOut.save(hold);
        publishHoldEvent("PARKING_SPACE_HOLD_CREATED", saved);
        return toResult(saved, true);
    }

    @Override
    @Transactional
    public void cancel(UUID holdId, String reason) {
        if (holdId == null) {
            throw new BadRequestException("holdId must not be null");
        }
        UUID operatorId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        CheckInSpaceHold hold = holdPortOut.findByIdForUpdate(holdId)
                .orElseThrow(() -> new NotFoundException("Check-in parking-space hold not found"));
        ensureOwner(hold, operatorId);
        policy.expireIfDue(hold, Instant.now());
        boolean wasHeld = hold.getStatus() == CheckInSpaceHoldStatus.HELD;
        policy.cancel(hold, TextValidationUtils.normalizeNullableText(reason, "reason", 100));
        holdPortOut.save(hold);
        if (wasHeld && hold.getStatus() == CheckInSpaceHoldStatus.CANCELLED) {
            publishHoldEvent("PARKING_SPACE_HOLD_CANCELLED", hold);
        }
    }

    @Override
    @Transactional
    public int expireDue(Instant now) {
        return expireDueBatches(now == null ? Instant.now() : now);
    }

    private int expireDueBatches(Instant now) {
        int totalExpired = 0;
        int batchSize;
        do {
            var dueHolds = holdPortOut.findDueForUpdate(now, EXPIRY_BATCH_SIZE);
            batchSize = dueHolds.size();
            dueHolds.forEach(hold -> {
                policy.expireIfDue(hold, now);
                holdPortOut.save(hold);
                publishHoldEvent("PARKING_SPACE_HOLD_EXPIRED", hold);
            });
            totalExpired += batchSize;
        } while (batchSize == EXPIRY_BATCH_SIZE);
        return totalExpired;
    }

    private void publishHoldEvent(String eventType, CheckInSpaceHold hold) {
        ParkingRealtimeAfterCommit.publishAfterCommit(realtimePublisher,
                new ParkingRealtimeEvent(eventType, hold.getParkingLotId(), null,
                        hold.getZoneId(), hold.getParkingSpaceId(), hold.getStatus().name(), null,
                        Map.of("holdId", hold.getHoldId().toString(),
                                "customerType", hold.getCustomerType())));
    }

    private ParkingSpace selectSpace(CheckInPreviewResult preview, Instant now) {
        var reserved = preview.eligibleSpaces().stream()
                .filter(item -> item.status() == ParkingSpaceStatus.RESERVED)
                .findFirst();
        if (reserved.isPresent()) {
            UUID spaceId = reserved.get().parkingSpaceId();
            ParkingSpace space = spacePortOut.findByIdForUpdate(spaceId)
                    .orElseThrow(() -> new NotFoundException("Allocated parking space not found"));
            if (!preview.zoneId().equals(space.getZoneId())
                    || !preview.vehicleTypeId().equals(space.getVehicleTypeId())
                    || space.getStatus() != ParkingSpaceStatus.RESERVED) {
                throw new ConflictException("Allocated parking space is no longer eligible for check-in hold");
            }
            if (holdPortOut.existsHeldByParkingSpaceId(spaceId)) {
                throw new ConflictException("Allocated parking space already has an active check-in hold");
            }
            return space;
        }
        return spacePortOut.findFirstVisitorAvailableForUpdate(
                        preview.zoneId(), preview.vehicleTypeId(), now)
                .orElseThrow(() -> new ConflictException("No compatible parking space is available for check-in hold"));
    }

    private Card resolvePhysicalCard(String cardUid, String subscriptionQrPayload) {
        if (subscriptionQrPayload != null && !subscriptionQrPayload.isBlank()) {
            return null;
        }
        String normalizedUid = TextValidationUtils.normalizeRequiredText(cardUid, "cardUid", 100);
        return cardPortOut.findByUidForUpdate(normalizedUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private String hashCredential(CreateCheckInSpaceHoldCommand command) {
        String credential = String.join("|",
                value(command.cardUid()), value(command.subscriptionQrPayload()),
                value(command.reservationQrPayload()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(credential.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private void ensureOwner(CheckInSpaceHold hold, UUID operatorId) {
        if (!operatorId.equals(hold.getOperatorAccountId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Check-in parking-space hold belongs to another operator");
        }
    }

    private CheckInSpaceHoldResult toResult(CheckInSpaceHold hold, boolean created) {
        ParkingSpace space = spacePortOut.findById(hold.getParkingSpaceId())
                .orElseThrow(() -> new NotFoundException("Parking space not found"));
        CheckInSpaceHoldStatus status = hold.getStatus();
        return new CheckInSpaceHoldResult(hold.getHoldId(), hold.getParkingLotId(), hold.getZoneId(),
                hold.getParkingSpaceId(), space.getCode(), hold.getCustomerType(), status,
                hold.getExpiresAt(), Instant.now(), created);
    }
}
