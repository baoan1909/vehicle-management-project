package com.ban.vehicle_management.infrastructure.mapper.catalog.holidaycalendar;

import com.ban.vehicle_management.domain.catalog.holidaycalendar.model.HolidayCalendar;
import com.ban.vehicle_management.infrastructure.persistence.catalog.holidaycalendar.HolidayCalendarEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class HolidayCalendarPersistenceMapperImpl implements HolidayCalendarPersistenceMapper {

    @Override
    public HolidayCalendarEntity toEntity(HolidayCalendar domain) {
        if ( domain == null ) {
            return null;
        }

        HolidayCalendarEntity holidayCalendarEntity = new HolidayCalendarEntity();

        holidayCalendarEntity.setCreatedAt( domain.getCreatedAt() );
        holidayCalendarEntity.setCreatedBy( domain.getCreatedBy() );
        holidayCalendarEntity.setUpdatedAt( domain.getUpdatedAt() );
        holidayCalendarEntity.setUpdatedBy( domain.getUpdatedBy() );
        holidayCalendarEntity.setHolidayId( domain.getHolidayId() );
        holidayCalendarEntity.setHolidayDate( domain.getHolidayDate() );
        holidayCalendarEntity.setName( domain.getName() );
        holidayCalendarEntity.setPriceMultiplier( domain.getPriceMultiplier() );

        return holidayCalendarEntity;
    }

    @Override
    public HolidayCalendar toDomain(HolidayCalendarEntity entity) {
        if ( entity == null ) {
            return null;
        }

        HolidayCalendar holidayCalendar = new HolidayCalendar();

        holidayCalendar.setCreatedAt( entity.getCreatedAt() );
        holidayCalendar.setCreatedBy( entity.getCreatedBy() );
        holidayCalendar.setUpdatedAt( entity.getUpdatedAt() );
        holidayCalendar.setUpdatedBy( entity.getUpdatedBy() );
        holidayCalendar.setHolidayId( entity.getHolidayId() );
        holidayCalendar.setHolidayDate( entity.getHolidayDate() );
        holidayCalendar.setName( entity.getName() );
        holidayCalendar.setPriceMultiplier( entity.getPriceMultiplier() );

        return holidayCalendar;
    }
}
