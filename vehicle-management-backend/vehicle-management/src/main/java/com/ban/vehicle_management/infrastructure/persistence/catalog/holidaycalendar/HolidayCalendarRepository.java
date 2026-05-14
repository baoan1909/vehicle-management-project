package com.ban.vehicle_management.infrastructure.persistence.catalog.holidaycalendar;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendarEntity, UUID> {
}
