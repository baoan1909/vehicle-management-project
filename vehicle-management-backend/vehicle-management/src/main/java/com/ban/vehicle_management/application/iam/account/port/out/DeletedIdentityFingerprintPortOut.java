package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.application.iam.account.model.result.DeletedIdentityMatch;
import com.ban.vehicle_management.shared.enumeration.iam.DeletedIdentityType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeletedIdentityFingerprintPortOut {
    void capture(UUID requestId, UUID accountId, Instant capturedAt);

    List<DeletedIdentityMatch> lookup(DeletedIdentityType identityType, String rawValue);
}
