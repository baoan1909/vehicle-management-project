package com.ban.vehicle_management.domain.operations.supportticketcategory.policy;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketAudienceType;
import com.ban.vehicle_management.shared.enumeration.operations.PermissionMatchMode;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.Set;

public class SupportTicketCategoryPolicy {

    public void initialize(SupportTicketCategory category){
        requiredCategory(category);
        if (category.getStatus() == null){
            category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        }
        applyDefaults(category);
        validateState(category);
    }

    public void activate(SupportTicketCategory category){
        requiredCategory(category);
        category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        validateState(category);
    }

    public void deactivate(SupportTicketCategory category){
        requiredCategory(category);
        category.setStatus(SupportTicketCategoryStatus.INACTIVE);
        validateState(category);
    }

    public void validateState(SupportTicketCategory category){
        requiredCategory(category);
        applyDefaults(category);
        category.setCode(TextValidationUtils.normalizeCode(category.getCode(), "code", 50));
        category.setName(TextValidationUtils.normalizeRequiredText(category.getName(), "name", 150));
        category.setDescription(TextValidationUtils.normalizeNullableText(category.getDescription(),"description", 0));
        category.setFeatureCode(category.getFeatureCode() == null || category.getFeatureCode().isBlank()
                ? null : TextValidationUtils.normalizeCode(category.getFeatureCode(), "featureCode", 100));

        if (category.getPriority() == null){
            throw new BadRequestException("Priority must not be null");
        }

        if (category.getStatus() == null){
            throw new BadRequestException("Status must not be null");
        }
        if (category.getAudienceType() == null) {
            throw new BadRequestException("Audience type must not be null");
        }
        if (category.getAudienceType() == SupportTicketAudienceType.ALL) {
            throw new BadRequestException("Support ticket category audience must be CUSTOMER or INTERNAL");
        }
        if (category.getDisplayOrder() == null || category.getDisplayOrder() < 0) {
            throw new BadRequestException("Display order must be non-negative");
        }
        category.setRequiredPermissionCodes(category.getRequiredPermissionCodes() == null
                ? Set.of()
                : category.getRequiredPermissionCodes().stream()
                        .map(code -> TextValidationUtils.normalizeCode(code, "requiredPermissionCode", 100))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
    }

    private void applyDefaults(SupportTicketCategory category) {
        if (category.getPermissionMatchMode() == null) {
            category.setPermissionMatchMode(PermissionMatchMode.ANY);
        }
        if (category.getDisplayOrder() == null) {
            category.setDisplayOrder(0);
        }
        if (category.getRequiredPermissionCodes() == null) {
            category.setRequiredPermissionCodes(Set.of());
        }
    }

    private void requiredCategory(SupportTicketCategory category){
        if (category == null){
            throw new BadRequestException("SupportTicketCategory must not be null");
        }
    }
}
