package com.ban.vehicle_management.domain.operations.shift.policy;

import com.ban.vehicle_management.domain.operations.shift.model.Shift;
import com.ban.vehicle_management.shared.enumeration.ShiftStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;

public class ShiftPolicy {

    public void initialize(Shift shift) {
        requireShift(shift);
        shift.setShiftCode(TextValidationUtils.normalizeCode(shift.getShiftCode(), "shiftCode", 50));
        requireField(shift.getParkingLotId(), "parkingLotId");
        requireField(shift.getStartTime(), "startTime");
        if (shift.getStatus() == null) {
            shift.setStatus(ShiftStatus.OPEN);
        }
        if (shift.getOpeningCash() == null) {
            shift.setOpeningCash(BigDecimal.ZERO);
        }
        validateState(shift);
    }

    public void close(Shift shift, Instant endTime, BigDecimal closingCash) {
        requireStatus(shift, ShiftStatus.OPEN);
        requireField(endTime, "endTime");
        requireField(closingCash, "closingCash");

        shift.setEndTime(endTime);
        shift.setClosingCash(closingCash);
        shift.setStatus(ShiftStatus.CLOSED);
        validateState(shift);
    }

    public void cancel(Shift shift) {
        requireStatus(shift, ShiftStatus.OPEN);
        shift.setStatus(ShiftStatus.CANCELLED);
        shift.setEndTime(null);
        shift.setClosingCash(null);
        validateState(shift);
    }

    public void validateState(Shift shift) {
        requireShift(shift);
        shift.setShiftCode(TextValidationUtils.normalizeCode(shift.getShiftCode(), "shiftCode", 50));
        requireField(shift.getParkingLotId(), "parkingLotId");
        requireField(shift.getStartTime(), "startTime");
        requireField(shift.getStatus(), "status");

        BigDecimal openingCash = shift.getOpeningCash() == null ? BigDecimal.ZERO : shift.getOpeningCash();
        if (openingCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("openingCash must not be negative");
        }
        shift.setOpeningCash(openingCash);

        if (shift.getClosingCash() != null && shift.getClosingCash().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("closingCash must not be negative");
        }

        switch (shift.getStatus()) {
            case OPEN -> {
                if (shift.getEndTime() != null || shift.getClosingCash() != null) {
                    throw new BadRequestException("Open shift must not have endTime or closingCash");
                }
            }
            case CLOSED -> {
                requireField(shift.getEndTime(), "endTime");
                requireField(shift.getClosingCash(), "closingCash");
                if (shift.getEndTime().isBefore(shift.getStartTime())) {
                    throw new BadRequestException("endTime must not be before startTime");
                }
            }
            case CANCELLED -> {
                if (shift.getClosingCash() != null) {
                    throw new BadRequestException("Cancelled shift must not have closingCash");
                }
            }
        }
    }

    private void requireStatus(Shift shift, ShiftStatus expectedStatus) {
        requireShift(shift);
        if (shift.getStatus() != expectedStatus) {
            throw new BadRequestException("Shift must be in " + expectedStatus + " status");
        }
    }

    private void requireShift(Shift shift) {
        requireField(shift, "shift");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

}

