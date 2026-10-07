package com.ban.vehicle_management.domain.iam.organization.policy;

import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.domain.shared.address.VietnamAddressPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Component;

@Component
public class OrganizationPolicy {

    private final VietnamAddressPolicy vietnamAddressPolicy;

    public OrganizationPolicy(VietnamAddressPolicy vietnamAddressPolicy) {
        this.vietnamAddressPolicy = vietnamAddressPolicy;
    }

    public void initialize(Organization organization) {
        if (organization == null) {
            throw new BadRequestException("organization must not be null");
        }
        organization.setCode(TextValidationUtils.normalizeCode(organization.getCode(), "code", 50));
        organization.setName(TextValidationUtils.normalizeRequiredText(organization.getName(), "name", 150));
        if (organization.getStatus() == null) {
            organization.setStatus(OrganizationStatus.ACTIVE);
        }
        validateStructuredAddress(organization);
    }

    private void validateStructuredAddress(Organization organization) {
        VietnamAddress structuredAddress = organization.getStructuredAddress();
        if (structuredAddress != null) {
            vietnamAddressPolicy.validateAndBuildDisplay(structuredAddress);
            organization.setStructuredAddress(structuredAddress);
        }
    }
}
