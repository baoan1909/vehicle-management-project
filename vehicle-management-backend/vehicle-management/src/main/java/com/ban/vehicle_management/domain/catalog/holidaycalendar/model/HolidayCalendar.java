package com.ban.vehicle_management.domain.catalog.holidaycalendar.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HolidayCalendar extends AuditableDomainModel {

    private UUID holidayId;
    private LocalDate holidayDate;
    private String name;
    private BigDecimal priceMultiplier;
}

