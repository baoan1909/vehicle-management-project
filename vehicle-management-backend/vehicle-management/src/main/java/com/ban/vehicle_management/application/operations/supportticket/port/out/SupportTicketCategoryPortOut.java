package com.ban.vehicle_management.application.operations.supportticket.port.out;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketCategoryPortOut {

    SupportTicketCategory save(SupportTicketCategory category);
    Optional<SupportTicketCategory> findById(UUID categoryId);
    List<SupportTicketCategory> findAll(SupportTicketCategoryStatus status, SupportTicketCategoryPriority priority, String keyword);
    boolean existsActiveByCode(String code);
    boolean existsActiveByCodeAndCategoryIdNot(String code, UUID categoryId);
    boolean hasUnfinishedTickets(UUID categoryId);
}
