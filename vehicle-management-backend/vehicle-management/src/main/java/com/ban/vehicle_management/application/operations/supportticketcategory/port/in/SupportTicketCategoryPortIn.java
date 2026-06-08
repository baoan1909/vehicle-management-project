package com.ban.vehicle_management.application.operations.supportticketcategory.port.in;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import java.util.List;
import java.util.UUID;

public interface SupportTicketCategoryPortIn {
    SupportTicketCategory createCategory(SupportTicketCategory category);
    SupportTicketCategory getCategoryById(UUID categoryId);
    List<SupportTicketCategory> getCategories(SupportTicketCategoryStatus status, SupportTicketCategoryPriority priority, String keyword);
    SupportTicketCategory updateCategory(UUID categoryId, SupportTicketCategory category);
    void deleteCategory(UUID categoryId);
    SupportTicketCategory activateCategory(UUID categoryId);
}