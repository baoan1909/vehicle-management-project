package com.ban.vehicle_management.application.operations.supportticketcategory.usecase;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.domain.operations.supportticketcategory.policy.SupportTicketCategoryPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketCategoryUseCaseImpl implements SupportTicketCategoryPortIn {

    private final SupportTicketCategoryPortOut categoryPortOut;
    private final SupportTicketCategoryPolicy categoryPolicy = new SupportTicketCategoryPolicy();

    public SupportTicketCategoryUseCaseImpl(SupportTicketCategoryPortOut categoryPortOut) {
        this.categoryPortOut = categoryPortOut;
    }

    @Override
    @Transactional
    public SupportTicketCategory createCategory(SupportTicketCategory category) {
        categoryPolicy.initialize(category);

        if (categoryPortOut.existsActiveByCode(category.getCode())) {
            throw new ConflictException("Active support ticket category code already exists");
        }

        category.setCategoryId(UUID.randomUUID());
        return categoryPortOut.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketCategory getCategoryById(UUID categoryId) {
        return categoryPortOut.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Support ticket category not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketCategory> getCategories(
            SupportTicketCategoryStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return categoryPortOut.findAll(status, priority, normalizeKeyword(keyword));
    }

    @Override
    @Transactional
    public SupportTicketCategory updateCategory(UUID categoryId, SupportTicketCategory category) {
        SupportTicketCategory existingCategory = getCategoryById(categoryId);

        existingCategory.setCode(category.getCode());
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setPriority(category.getPriority());

        categoryPolicy.validateState(existingCategory);

        if (existingCategory.getStatus() == SupportTicketCategoryStatus.ACTIVE
                && categoryPortOut.existsActiveByCodeAndCategoryIdNot(existingCategory.getCode(), categoryId)) {
            throw new ConflictException("Active support ticket category code already exists");
        }

        return categoryPortOut.save(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) {
        SupportTicketCategory existingCategory = getCategoryById(categoryId);

        if (existingCategory.getStatus() == SupportTicketCategoryStatus.INACTIVE) {
            return;
        }

        if (categoryPortOut.hasUnfinishedTickets(categoryId)) {
            throw new ConflictException("Cannot deactivate category with unfinished support tickets");
        }

        categoryPolicy.deactivate(existingCategory);
        categoryPortOut.save(existingCategory);
    }

    @Override
    @Transactional
    public SupportTicketCategory activateCategory(UUID categoryId) {
        SupportTicketCategory existingCategory = getCategoryById(categoryId);

        if (existingCategory.getStatus() == SupportTicketCategoryStatus.ACTIVE) {
            return existingCategory;
        }

        categoryPolicy.activate(existingCategory);

        if (categoryPortOut.existsActiveByCodeAndCategoryIdNot(existingCategory.getCode(), categoryId)) {
            throw new ConflictException("Active support ticket category code already exists");
        }

        return categoryPortOut.save(existingCategory);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}