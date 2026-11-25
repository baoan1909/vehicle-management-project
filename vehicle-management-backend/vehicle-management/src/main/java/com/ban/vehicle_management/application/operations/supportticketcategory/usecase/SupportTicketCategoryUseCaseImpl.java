package com.ban.vehicle_management.application.operations.supportticketcategory.usecase;

import com.ban.vehicle_management.application.operations.supportticketcategory.port.out.SupportTicketCategoryPortOut;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.permission.port.out.PermissionPortOut;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.domain.operations.supportticketcategory.policy.SupportTicketCategoryPolicy;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.exception.ScopedAccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketCategoryUseCaseImpl implements SupportTicketCategoryPortIn {

    private static final java.util.Comparator<SupportTicketCategory> CATEGORY_ORDER = java.util.Comparator
            .comparing(SupportTicketCategory::getDisplayOrder, java.util.Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SupportTicketCategory::getCode, java.util.Comparator.nullsLast(String::compareTo));

    private final SupportTicketCategoryPortOut categoryPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final PermissionPortOut permissionPortOut;
    private final SupportTicketCategoryPolicy categoryPolicy = new SupportTicketCategoryPolicy();

    @Autowired
    public SupportTicketCategoryUseCaseImpl(
            SupportTicketCategoryPortOut categoryPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            PermissionPortOut permissionPortOut
    ) {
        this.categoryPortOut = categoryPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.permissionPortOut = permissionPortOut;
    }

    public SupportTicketCategoryUseCaseImpl(
            SupportTicketCategoryPortOut categoryPortOut,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this(categoryPortOut, currentAccountPortIn, null);
    }

    public SupportTicketCategoryUseCaseImpl(SupportTicketCategoryPortOut categoryPortOut) {
        this(categoryPortOut, null, null);
    }

    @Override
    @Transactional
    public SupportTicketCategory createCategory(SupportTicketCategory category) {
        categoryPolicy.initialize(category);
        validateRequiredPermissions(category);

        if (categoryPortOut.existsActiveByCode(category.getCode())) {
            throw new ConflictException("Support ticket category code already exists");
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
    public boolean isActiveCategory(UUID categoryId) {
        return categoryPortOut.existsByIdAndStatus(categoryId, SupportTicketCategoryStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketCategory> getCategories(
            SupportTicketCategoryStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    ) {
        return categoryPortOut.findAll(status, priority, normalizeKeyword(keyword)).stream()
                .sorted(CATEGORY_ORDER).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketCategory> getAvailableCategories() {
        CurrentAccountAccess access = currentAccess();
        requireCreatePermission();
        return categoryPortOut.findAll(SupportTicketCategoryStatus.ACTIVE, null, null).stream()
                .filter(category -> category.getStatus() == SupportTicketCategoryStatus.ACTIVE)
                .filter(category -> categoryPolicy.matchesPermissions(category, access.getEffectivePermissionCodes()))
                .sorted(CATEGORY_ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketCategory> getPublicCategories() {
        return categoryPortOut.findAll(SupportTicketCategoryStatus.ACTIVE, null, null).stream()
                .filter(categoryPolicy::isPubliclyAvailable).sorted(CATEGORY_ORDER).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void ensurePublicAvailableCategory(UUID categoryId) {
        if (!categoryPortOut.existsPublicAvailableCategory(categoryId)) {
            throw new BadRequestException("Danh mục hỗ trợ không khả dụng. Vui lòng chọn lại danh mục.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketCategory ensureAvailableCategory(UUID categoryId) {
        CurrentAccountAccess access = currentAccess();
        requireCreatePermission();
        SupportTicketCategory category = getCategoryById(categoryId);
        if (!categoryPolicy.matchesPermissions(category, access.getEffectivePermissionCodes())) {
            throw new ScopedAccessDeniedException("SUPPORT_CATEGORY_NOT_AVAILABLE",
                    "Bạn không có quyền sử dụng danh mục hỗ trợ này. Vui lòng chọn lại danh mục.");
        }
        if (category.getStatus() != SupportTicketCategoryStatus.ACTIVE) {
            throw new ConflictException("Danh mục hỗ trợ đã bị vô hiệu hóa. Vui lòng tải lại và chọn danh mục khác.");
        }
        return category;
    }

    @Override
    @Transactional
    public SupportTicketCategory updateCategory(UUID categoryId, SupportTicketCategory category) {
        SupportTicketCategory existingCategory = getCategoryById(categoryId);

        if (categoryPortOut.hasTickets(categoryId)
                && (!existingCategory.getCode().equalsIgnoreCase(category.getCode())
                || existingCategory.getAudienceType() != category.getAudienceType())) {
            throw new ConflictException("Code and audience cannot be changed after the category has been used");
        }

        existingCategory.setCode(category.getCode());
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setPriority(category.getPriority());
        existingCategory.setAudienceType(category.getAudienceType());
        existingCategory.setFeatureCode(category.getFeatureCode());
        existingCategory.setPermissionMatchMode(category.getPermissionMatchMode());
        existingCategory.setDisplayOrder(category.getDisplayOrder());
        existingCategory.setRequiredPermissionCodes(category.getRequiredPermissionCodes());

        categoryPolicy.validateState(existingCategory);
        validateRequiredPermissions(existingCategory);

        if (categoryPortOut.existsActiveByCodeAndCategoryIdNot(existingCategory.getCode(), categoryId)) {
            throw new ConflictException("Support ticket category code already exists");
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
        validateRequiredPermissions(existingCategory);

        return categoryPortOut.save(existingCategory);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private CurrentAccountAccess currentAccess() {
        if (currentAccountPortIn == null) {
            throw new IllegalStateException("Current account access is required");
        }
        return currentAccountPortIn.getCurrentAccountOrThrow();
    }

    private void requireCreatePermission() {
        currentAccountPortIn.requirePermission("SUPPORT_TICKET_CREATE_OWN");
    }

    private void validateRequiredPermissions(SupportTicketCategory category) {
        if (permissionPortOut == null || category.getRequiredPermissionCodes().isEmpty()) {
            return;
        }
        java.util.Set<String> existingCodes = permissionPortOut.findAll(null).stream()
                .map(com.ban.vehicle_management.domain.iam.permission.model.Permission::getPermissionCode)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<String> unknownCodes = category.getRequiredPermissionCodes().stream()
                .filter(code -> !existingCodes.contains(code))
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
        if (!unknownCodes.isEmpty()) {
            throw new BadRequestException("Unknown permission codes: " + String.join(", ", unknownCodes));
        }
    }

}
