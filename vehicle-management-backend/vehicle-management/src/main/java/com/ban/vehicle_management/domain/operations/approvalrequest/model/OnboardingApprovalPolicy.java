package com.ban.vehicle_management.domain.operations.approvalrequest.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OnboardingApprovalPolicy extends AuditableDomainModel {

    private UUID policyId;
    private OnboardingApprovalPolicyType policyType;
    private boolean autoApproveEnabled;
    private Instant effectiveFrom;
    private Long version;

    public boolean updateEnabled(boolean enabled, UUID actorAccountId, Instant changedAt) {
        if (autoApproveEnabled == enabled) {
            return false;
        }
        autoApproveEnabled = enabled;
        effectiveFrom = enabled ? changedAt : null;
        setUpdatedBy(actorAccountId);
        setUpdatedAt(changedAt);
        return true;
    }

    public boolean isEligible(Instant requestCreatedAt) {
        return autoApproveEnabled
                && effectiveFrom != null
                && requestCreatedAt != null
                && !requestCreatedAt.isBefore(effectiveFrom);
    }
}
