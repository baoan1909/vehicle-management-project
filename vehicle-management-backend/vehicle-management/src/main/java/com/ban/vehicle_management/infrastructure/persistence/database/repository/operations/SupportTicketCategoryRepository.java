package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketCategoryEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SupportTicketCategoryRepository
    extends JpaRepository<SupportTicketCategoryEntity, UUID>, JpaSpecificationExecutor<SupportTicketCategoryEntity> {
    boolean existsByCodeAndStatus(String code, SupportTicketCategoryStatus status);

    boolean existsByCodeAndStatusAndCategoryIdNot(
            String code,
            SupportTicketCategoryStatus status,
            UUID categoryId
    );

}
