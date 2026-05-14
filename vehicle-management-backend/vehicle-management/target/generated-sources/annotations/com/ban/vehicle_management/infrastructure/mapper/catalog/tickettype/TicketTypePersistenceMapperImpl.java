package com.ban.vehicle_management.infrastructure.mapper.catalog.tickettype;

import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.infrastructure.persistence.catalog.tickettype.TicketTypeEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class TicketTypePersistenceMapperImpl implements TicketTypePersistenceMapper {

    @Override
    public TicketTypeEntity toEntity(TicketType domain) {
        if ( domain == null ) {
            return null;
        }

        TicketTypeEntity ticketTypeEntity = new TicketTypeEntity();

        ticketTypeEntity.setCreatedAt( domain.getCreatedAt() );
        ticketTypeEntity.setCreatedBy( domain.getCreatedBy() );
        ticketTypeEntity.setUpdatedAt( domain.getUpdatedAt() );
        ticketTypeEntity.setUpdatedBy( domain.getUpdatedBy() );
        ticketTypeEntity.setTicketTypeId( domain.getTicketTypeId() );
        ticketTypeEntity.setCode( domain.getCode() );
        ticketTypeEntity.setName( domain.getName() );
        ticketTypeEntity.setDescription( domain.getDescription() );
        ticketTypeEntity.setDurationDays( domain.getDurationDays() );
        ticketTypeEntity.setIsActive( domain.getIsActive() );

        return ticketTypeEntity;
    }

    @Override
    public TicketType toDomain(TicketTypeEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TicketType ticketType = new TicketType();

        ticketType.setCreatedAt( entity.getCreatedAt() );
        ticketType.setCreatedBy( entity.getCreatedBy() );
        ticketType.setUpdatedAt( entity.getUpdatedAt() );
        ticketType.setUpdatedBy( entity.getUpdatedBy() );
        ticketType.setTicketTypeId( entity.getTicketTypeId() );
        ticketType.setCode( entity.getCode() );
        ticketType.setName( entity.getName() );
        ticketType.setDescription( entity.getDescription() );
        ticketType.setDurationDays( entity.getDurationDays() );
        ticketType.setIsActive( entity.getIsActive() );

        return ticketType;
    }
}
