package com.ban.vehicle_management.infrastructure.mapper.operations.supportticket;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.infrastructure.persistence.operations.supportticket.SupportTicketEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-14T12:41:12+0700",
    comments = "version: 1.6.3, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class SupportTicketPersistenceMapperImpl implements SupportTicketPersistenceMapper {

    @Override
    public SupportTicketEntity toEntity(SupportTicket domain) {
        if ( domain == null ) {
            return null;
        }

        SupportTicketEntity supportTicketEntity = new SupportTicketEntity();

        supportTicketEntity.setCreatedAt( domain.getCreatedAt() );
        supportTicketEntity.setCreatedBy( domain.getCreatedBy() );
        supportTicketEntity.setUpdatedAt( domain.getUpdatedAt() );
        supportTicketEntity.setUpdatedBy( domain.getUpdatedBy() );
        supportTicketEntity.setSupportTicketId( domain.getSupportTicketId() );
        supportTicketEntity.setCustomerId( domain.getCustomerId() );
        supportTicketEntity.setTitle( domain.getTitle() );
        supportTicketEntity.setContent( domain.getContent() );
        supportTicketEntity.setStatus( domain.getStatus() );
        supportTicketEntity.setPriority( domain.getPriority() );
        supportTicketEntity.setAssignedTo( domain.getAssignedTo() );
        supportTicketEntity.setResolvedAt( domain.getResolvedAt() );

        return supportTicketEntity;
    }

    @Override
    public SupportTicket toDomain(SupportTicketEntity entity) {
        if ( entity == null ) {
            return null;
        }

        SupportTicket supportTicket = new SupportTicket();

        supportTicket.setCreatedAt( entity.getCreatedAt() );
        supportTicket.setCreatedBy( entity.getCreatedBy() );
        supportTicket.setUpdatedAt( entity.getUpdatedAt() );
        supportTicket.setUpdatedBy( entity.getUpdatedBy() );
        supportTicket.setSupportTicketId( entity.getSupportTicketId() );
        supportTicket.setCustomerId( entity.getCustomerId() );
        supportTicket.setTitle( entity.getTitle() );
        supportTicket.setContent( entity.getContent() );
        supportTicket.setStatus( entity.getStatus() );
        supportTicket.setPriority( entity.getPriority() );
        supportTicket.setAssignedTo( entity.getAssignedTo() );
        supportTicket.setResolvedAt( entity.getResolvedAt() );

        return supportTicket;
    }
}
