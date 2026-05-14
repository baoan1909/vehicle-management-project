package com.ban.vehicle_management.infrastructure.mapper.catalog.holidaycalendar;

import com.ban.vehicle_management.domain.catalog.holidaycalendar.model.HolidayCalendar;
import com.ban.vehicle_management.infrastructure.persistence.catalog.holidaycalendar.HolidayCalendarEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HolidayCalendarPersistenceMapper {

    HolidayCalendarEntity toEntity(HolidayCalendar domain);

    HolidayCalendar toDomain(HolidayCalendarEntity entity);
}
