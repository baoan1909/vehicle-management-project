package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ParkingCheckoutPricePolicy {

    private static final Duration SHORT_STAY_THRESHOLD = Duration.ofHours(4);
    private static final Duration FULL_DAY = Duration.ofHours(24);

    public BigDecimal calculateVisitorPrice(
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            BigDecimal dayPrice,
            BigDecimal nightPrice,
            LocalTime dayStart,
            LocalTime dayEnd
    ) {
        requireField(checkInTime, "checkInTime");
        requireField(checkOutTime, "checkOutTime");
        requirePrice(dayPrice, "dayPrice");
        requirePrice(nightPrice, "nightPrice");
        requireField(dayStart, "dayStart");
        requireField(dayEnd, "dayEnd");

        if (!checkOutTime.isAfter(checkInTime)) {
            throw new BadRequestException("checkOutTime must be after checkInTime");
        }
        if (!dayStart.isBefore(dayEnd)) {
            throw new BadRequestException("day price rule must not cross midnight");
        }

        Duration duration = Duration.between(checkInTime, checkOutTime);
        long fullDays = duration.toSeconds() / FULL_DAY.toSeconds();
        Duration remaining = duration.minusSeconds(fullDays * FULL_DAY.toSeconds());

        BigDecimal totalPrice = dayPrice.add(nightPrice).multiply(BigDecimal.valueOf(fullDays));
        if (remaining.isZero()) {
            return totalPrice;
        }

        LocalDateTime remainingStart = checkInTime.plusSeconds(fullDays * FULL_DAY.toSeconds());
        return totalPrice.add(calculateLessThanFullDay(
                remainingStart,
                checkOutTime,
                remaining,
                dayPrice,
                nightPrice,
                dayStart,
                dayEnd
        ));
    }

    private BigDecimal calculateLessThanFullDay(
            LocalDateTime start,
            LocalDateTime end,
            Duration duration,
            BigDecimal dayPrice,
            BigDecimal nightPrice,
            LocalTime dayStart,
            LocalTime dayEnd
    ) {
        if (duration.compareTo(SHORT_STAY_THRESHOLD) <= 0) {
            return dayPrice;
        }
        if (isFullyInsideDayShift(start, end, dayStart, dayEnd)) {
            return dayPrice;
        }
        return nightPrice;
    }

    private boolean isFullyInsideDayShift(
            LocalDateTime start,
            LocalDateTime end,
            LocalTime dayStart,
            LocalTime dayEnd
    ) {
        return start.toLocalDate().equals(end.toLocalDate())
                && !start.toLocalTime().isBefore(dayStart)
                && !end.toLocalTime().isAfter(dayEnd);
    }

    private void requirePrice(BigDecimal value, String fieldName) {
        requireField(value, fieldName);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(fieldName + " must not be negative");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
