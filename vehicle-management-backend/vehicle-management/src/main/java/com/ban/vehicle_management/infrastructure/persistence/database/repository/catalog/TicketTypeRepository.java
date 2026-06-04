package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.TicketTypeEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketTypeRepository
        extends JpaRepository<TicketTypeEntity, UUID>, JpaSpecificationExecutor<TicketTypeEntity> {

    boolean existsByCodeAndStatus(String code, TicketTypeStatus status);

    boolean existsByCodeAndStatusAndTicketTypeIdNot(
            String code,
            TicketTypeStatus status,
            UUID ticketTypeId
    );
}