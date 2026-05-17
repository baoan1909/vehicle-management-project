package com.ban.vehicle_management.infrastructure.mapper.catalog;

import com.ban.vehicle_management.domain.catalog.holidaycalendar.model.HolidayCalendar;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.HolidayCalendarEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HolidayCalendarPersistenceMapper {

    HolidayCalendarEntity toEntity(HolidayCalendar domain);

    HolidayCalendar toDomain(HolidayCalendarEntity entity);
}


