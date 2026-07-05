package com.ban.vehicle_management.application.operations.supportticket.port.out;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketPortOut {
    SupportTicket save(SupportTicket supportTicket);
    Optional<SupportTicket> findById(UUID supportTicketId);

    List<SupportTicket> findAll(
            UUID customerId,
            UUID categoryId,
            UUID assignedTo,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    );

    boolean existsActiveCategoryById(UUID categoryId);
    boolean existsAssignableAccountById(UUID accountId);
    boolean existsActiveWorkflowByCustomerIdAndCategoryId(UUID customerId, UUID categoryId);
}
