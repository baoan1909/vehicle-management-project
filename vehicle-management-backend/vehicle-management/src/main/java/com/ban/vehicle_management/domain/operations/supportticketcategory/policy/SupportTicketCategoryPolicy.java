package com.ban.vehicle_management.domain.operations.supportticketcategory.policy;

import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

public class SupportTicketCategoryPolicy {

    public void initialize(SupportTicketCategory category){
        requiredCategory(category);
        if (category.getStatus() == null){
            category.setStatus(SupportTicketCategoryStatus.ACTIVE);
        }
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
        category.setCode(TextValidationUtils.normalizeCode(category.getCode(), "code", 50));
        category.setName(TextValidationUtils.normalizeRequiredText(category.getName(), "name", 150));
        category.setDescription(TextValidationUtils.normalizeNullableText(category.getDescription(),"description", 0));

        if (category.getPriority() == null){
            throw new BadRequestException("Priority must not be null");
        }

        if (category.getStatus() == null){
            throw new BadRequestException("Status must not be null");
        }
    }
    private void requiredCategory(SupportTicketCategory category){
        if (category == null){
            throw new BadRequestException("SupportTicketCategory must not be null");
        }
    }
}
