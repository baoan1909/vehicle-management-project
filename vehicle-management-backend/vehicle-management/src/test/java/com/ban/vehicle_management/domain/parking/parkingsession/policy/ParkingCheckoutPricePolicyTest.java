package com.ban.vehicle_management.domain.parking.parkingsession.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ParkingCheckoutPricePolicyTest {

    private final ParkingCheckoutPricePolicy parkingCheckoutPricePolicy = new ParkingCheckoutPricePolicy();

    @Test
    void shouldChargeDayPriceWhenDurationIsNotGreaterThanFourHours() {
        BigDecimal price = calculate(
                LocalDateTime.of(2026, 6, 25, 20, 0),
                LocalDateTime.of(2026, 6, 25, 23, 30)
        );

        assertEquals(new BigDecimal("5000"), price);
    }

    @Test
    void shouldChargeDayPriceWhenSessionLongerThanFourHoursButFullyInsideDayShift() {
        BigDecimal price = calculate(
                LocalDateTime.of(2026, 6, 25, 7, 0),
                LocalDateTime.of(2026, 6, 25, 16, 0)
        );

        assertEquals(new BigDecimal("5000"), price);
    }

    @Test
    void shouldChargeNightPriceWhenSessionLongerThanFourHoursTouchesNightShift() {
        BigDecimal price = calculate(
                LocalDateTime.of(2026, 6, 25, 17, 0),
                LocalDateTime.of(2026, 6, 25, 21, 30)
        );

        assertEquals(new BigDecimal("10000"), price);
    }

    @Test
    void shouldChargeFullDayAndRemainingPriceForMultipleDays() {
        BigDecimal price = calculate(
                LocalDateTime.of(2026, 6, 25, 6, 20),
                LocalDateTime.of(2026, 6, 30, 13, 0)
        );

        assertEquals(new BigDecimal("80000"), price);
    }

    @Test
    void shouldRejectWhenCheckOutIsNotAfterCheckIn() {
        assertThrows(BadRequestException.class, () -> calculate(
                LocalDateTime.of(2026, 6, 25, 7, 0),
                LocalDateTime.of(2026, 6, 25, 7, 0)
        ));
    }

    private BigDecimal calculate(LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        return parkingCheckoutPricePolicy.calculateVisitorPrice(
                checkInTime,
                checkOutTime,
                new BigDecimal("5000"),
                new BigDecimal("10000"),
                LocalTime.of(6, 0),
                LocalTime.of(19, 59, 59)
        );
    }
}
