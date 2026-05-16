package com.ban.vehicle_management.domain.catalog.holidaycalendar.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.catalog.holidaycalendar.model.HolidayCalendar;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HolidayCalendarPolicyTest {

    private final HolidayCalendarPolicy holidayCalendarPolicy = new HolidayCalendarPolicy();

    @Test
    void shouldInitializeHolidayCalendar() {
        HolidayCalendar holidayCalendar = new HolidayCalendar();
        holidayCalendar.setHolidayDate(LocalDate.of(2026, 1, 1));
        holidayCalendar.setName(" Tet Duong lich ");
        holidayCalendar.setPriceMultiplier(new BigDecimal("1.50"));

        holidayCalendarPolicy.initialize(holidayCalendar);

        assertEquals("Tet Duong lich", holidayCalendar.getName());
        assertEquals(new BigDecimal("1.50"), holidayCalendar.getPriceMultiplier());
    }

    @Test
    void shouldRejectNonPositiveMultiplier() {
        HolidayCalendar holidayCalendar = new HolidayCalendar();
        holidayCalendar.setHolidayDate(LocalDate.of(2026, 1, 1));
        holidayCalendar.setName("Tet");
        holidayCalendar.setPriceMultiplier(BigDecimal.ZERO);

        assertThrows(BadRequestException.class, () -> holidayCalendarPolicy.initialize(holidayCalendar));
    }
}

