package com.ban.vehicle_management.domain.operations.shift.policy;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ShiftPolicy {

    public void initializeDraft(Shift shift) {
        requireShift(shift);

        shift.setStatus(ShiftStatus.DRAFT);
        clearApproval(shift);
        clearOpening(shift);
        clearClosing(shift);
        clearCancellation(shift);

        validateState(shift);
    }

    public void approve(
            Shift shift,
            UUID approvedBy,
            Instant approvedAt
    ) {
        requireStatus(shift, ShiftStatus.DRAFT);
        requireField(approvedBy, "approvedBy");
        requireField(approvedAt, "approvedAt");

        shift.setStatus(ShiftStatus.SCHEDULED);
        shift.setApprovedBy(approvedBy);
        shift.setApprovedAt(approvedAt);

        validateState(shift);
    }

    public void open(
            Shift shift,
            BigDecimal openingCash,
            UUID openedBy,
            Instant openedAt,
            String note
    ) {
        requireStatus(shift, ShiftStatus.SCHEDULED);
        requireField(openingCash, "openingCash");
        requireField(openedBy, "openedBy");
        requireField(openedAt, "openedAt");

        validateNonNegative(openingCash, "openingCash");

        if (openedAt.isBefore(shift.getStartTime())
                || !openedAt.isBefore(shift.getEndTime())) {
            throw new ConflictException(
                    "Shift can only be opened between startTime and endTime"
            );
        }

        shift.setStatus(ShiftStatus.OPEN);
        shift.setOpeningCash(openingCash);
        shift.setOpenedBy(openedBy);
        shift.setOpenedAt(openedAt);
        updateNote(shift, note);

        validateState(shift);
    }

    public void close(
            Shift shift,
            BigDecimal closingCash,
            UUID closedBy,
            Instant closedAt,
            String note
    ) {
        requireStatus(shift, ShiftStatus.OPEN);
        requireField(closingCash, "closingCash");
        requireField(closedBy, "closedBy");
        requireField(closedAt, "closedAt");

        validateNonNegative(closingCash, "closingCash");

        shift.setStatus(ShiftStatus.CLOSED);
        shift.setClosingCash(closingCash);
        shift.setClosedBy(closedBy);
        shift.setClosedAt(closedAt);
        updateNote(shift, note);

        validateState(shift);
    }

    public void cancel(
            Shift shift,
            String reason,
            UUID cancelledBy,
            Instant cancelledAt
    ) {
        requireShift(shift);

        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            return;
        }

        if (shift.getStatus() != ShiftStatus.DRAFT
                && shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new ConflictException(
                    "Only DRAFT or SCHEDULED shift can be cancelled"
            );
        }

        requireField(cancelledBy, "cancelledBy");
        requireField(cancelledAt, "cancelledAt");

        shift.setStatus(ShiftStatus.CANCELLED);
        shift.setCancelledBy(cancelledBy);
        shift.setCancelledAt(cancelledAt);
        shift.setCancellationReason(
                TextValidationUtils.normalizeRequiredText(
                        reason,
                        "reason",
                        0
                )
        );

        validateState(shift);
    }

    public void validateState(Shift shift) {
        requireShift(shift);

        requireField(shift.getShiftId(), "shiftId");
        requireField(shift.getShiftTemplateId(), "shiftTemplateId");
        requireField(shift.getParkingLotId(), "parkingLotId");
        requireField(shift.getShiftDate(), "shiftDate");
        requireField(shift.getShiftType(), "shiftType");
        requireField(shift.getStartTime(), "startTime");
        requireField(shift.getEndTime(), "endTime");
        requireField(shift.getStatus(), "status");

        shift.setShiftCode(
                TextValidationUtils.normalizeCode(
                        shift.getShiftCode(),
                        "shiftCode",
                        50
                )
        );

        shift.setNote(
                TextValidationUtils.normalizeNullableText(
                        shift.getNote(),
                        "note",
                        0
                )
        );

        if (!shift.getEndTime().isAfter(shift.getStartTime())) {
            throw new BadRequestException(
                    "endTime must be after startTime"
            );
        }

        validateNullableNonNegative(
                shift.getOpeningCash(),
                "openingCash"
        );
        validateNullableNonNegative(
                shift.getClosingCash(),
                "closingCash"
        );

        validateLifecycleState(shift);
    }

    private void validateLifecycleState(Shift shift) {
        switch (shift.getStatus()) {
            case DRAFT -> {
                requireNoApproval(shift);
                requireNoOpening(shift);
                requireNoClosing(shift);
                requireNoCancellation(shift);
            }
            case SCHEDULED -> {
                requireApproval(shift);
                requireNoOpening(shift);
                requireNoClosing(shift);
                requireNoCancellation(shift);
            }
            case OPEN -> {
                requireApproval(shift);
                requireOpening(shift);
                requireNoClosing(shift);
                requireNoCancellation(shift);
            }
            case CLOSED -> {
                requireApproval(shift);
                requireOpening(shift);
                requireClosing(shift);
                requireNoCancellation(shift);
            }
            case CANCELLED -> {
                requireCancellation(shift);
                requireNoOpening(shift);
                requireNoClosing(shift);
            }
        }
    }

    private void requireApproval(Shift shift) {
        requireField(shift.getApprovedAt(), "approvedAt");
        requireField(shift.getApprovedBy(), "approvedBy");
    }

    private void requireOpening(Shift shift) {
        requireField(shift.getOpeningCash(), "openingCash");
        requireField(shift.getOpenedAt(), "openedAt");
        requireField(shift.getOpenedBy(), "openedBy");
    }

    private void requireClosing(Shift shift) {
        requireField(shift.getClosingCash(), "closingCash");
        requireField(shift.getClosedAt(), "closedAt");
        requireField(shift.getClosedBy(), "closedBy");
    }

    private void requireCancellation(Shift shift) {
        requireField(shift.getCancelledAt(), "cancelledAt");
        requireField(shift.getCancelledBy(), "cancelledBy");
        shift.setCancellationReason(
                TextValidationUtils.normalizeRequiredText(
                        shift.getCancellationReason(),
                        "cancellationReason",
                        0
                )
        );
    }

    private void requireNoApproval(Shift shift) {
        if (shift.getApprovedAt() != null
                || shift.getApprovedBy() != null) {
            throw new BadRequestException(
                    "Shift must not contain approval data"
            );
        }
    }

    private void requireNoOpening(Shift shift) {
        if (shift.getOpeningCash() != null
                || shift.getOpenedAt() != null
                || shift.getOpenedBy() != null) {
            throw new BadRequestException(
                    "Shift must not contain opening data"
            );
        }
    }

    private void requireNoClosing(Shift shift) {
        if (shift.getClosingCash() != null
                || shift.getClosedAt() != null
                || shift.getClosedBy() != null) {
            throw new BadRequestException(
                    "Shift must not contain closing data"
            );
        }
    }

    private void requireNoCancellation(Shift shift) {
        if (shift.getCancelledAt() != null
                || shift.getCancelledBy() != null
                || shift.getCancellationReason() != null) {
            throw new BadRequestException(
                    "Shift must not contain cancellation data"
            );
        }
    }

    private void clearApproval(Shift shift) {
        shift.setApprovedAt(null);
        shift.setApprovedBy(null);
    }

    private void clearOpening(Shift shift) {
        shift.setOpeningCash(null);
        shift.setOpenedAt(null);
        shift.setOpenedBy(null);
    }

    private void clearClosing(Shift shift) {
        shift.setClosingCash(null);
        shift.setClosedAt(null);
        shift.setClosedBy(null);
    }

    private void clearCancellation(Shift shift) {
        shift.setCancelledAt(null);
        shift.setCancelledBy(null);
        shift.setCancellationReason(null);
    }

    private void updateNote(Shift shift, String note) {
        if (note != null) {
            shift.setNote(
                    TextValidationUtils.normalizeNullableText(
                            note,
                            "note",
                            0
                    )
            );
        }
    }

    private void validateNullableNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value != null) {
            validateNonNegative(value, fieldName);
        }
    }

    private void validateNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    fieldName + " must not be negative"
            );
        }
    }

    private void requireStatus(
            Shift shift,
            ShiftStatus expectedStatus
    ) {
        requireShift(shift);

        if (shift.getStatus() != expectedStatus) {
            throw new ConflictException(
                    "Shift must be in "
                            + expectedStatus
                            + " status"
            );
        }
    }

    private void requireShift(Shift shift) {
        requireField(shift, "shift");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(
                    fieldName + " must not be null"
            );
        }
    }
}