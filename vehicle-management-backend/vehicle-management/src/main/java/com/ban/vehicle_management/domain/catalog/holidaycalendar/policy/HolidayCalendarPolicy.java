package com.ban.vehicle_management.domain.catalog.holidaycalendar.policy;

import com.ban.vehicle_management.domain.catalog.holidaycalendar.model.HolidayCalendar;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;

public class HolidayCalendarPolicy {

    public void initialize(HolidayCalendar holidayCalendar) {
        requireHolidayCalendar(holidayCalendar);
        requireField(holidayCalendar.getHolidayDate(), "holidayDate");
        holidayCalendar.setName(TextValidationUtils.normalizeRequiredText(holidayCalendar.getName(), "name", 150));
        requireField(holidayCalendar.getPriceMultiplier(), "priceMultiplier");

        if (holidayCalendar.getPriceMultiplier().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("priceMultiplier must be greater than zero");
        }
    }

    private void requireHolidayCalendar(HolidayCalendar holidayCalendar) {
        if (holidayCalendar == null) {
            throw new BadRequestException("holidayCalendar must not be null");
        }
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}

