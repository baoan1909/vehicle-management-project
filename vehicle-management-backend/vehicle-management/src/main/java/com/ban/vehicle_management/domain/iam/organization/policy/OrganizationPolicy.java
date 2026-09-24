package com.ban.vehicle_management.domain.iam.organization.policy;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

@Component
public class OrganizationPolicy {

    public void initialize(Organization organization) {
        if (organization == null) {
            throw new BadRequestException("organization must not be null");
        }
        organization.setCode(TextValidationUtils.normalizeCode(organization.getCode(), "code", 50));
        organization.setName(TextValidationUtils.normalizeRequiredText(organization.getName(), "name", 150));
        organization.setAddress(TextValidationUtils.normalizeNullableText(organization.getAddress(), "address", 1000));
        if (organization.getStatus() == null) {
            organization.setStatus(OrganizationStatus.ACTIVE);
        }
    }
}
