package com.ban.vehicle_management.domain.operations.approvalrequest.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvatarApprovalPolicy extends AuditableDomainModel {
    private UUID policyId;
    private boolean autoApproveEnabled;
    private Instant effectiveFrom;
    private Long version;

    public boolean updateEnabled(boolean enabled, UUID actorAccountId, Instant changedAt) {
        if (autoApproveEnabled == enabled) return false;
        autoApproveEnabled = enabled;
        effectiveFrom = enabled ? changedAt : null;
        setUpdatedBy(actorAccountId);
        setUpdatedAt(changedAt);
        return true;
    }

    public boolean isEligible(Instant submittedAt) {
        return autoApproveEnabled && effectiveFrom != null && submittedAt != null
                && !submittedAt.isBefore(effectiveFrom);
    }
}
