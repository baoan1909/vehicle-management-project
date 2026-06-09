package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupportTicketRepository
        extends JpaRepository<SupportTicketEntity, UUID>, JpaSpecificationExecutor<SupportTicketEntity> {

    boolean existsByCategoryIdAndStatusIn(UUID categoryId, Collection<SupportTicketStatus> statuses);
}