package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.OrganizationClosureImpact;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrganizationClosurePortOut {
    List<OrganizationClosureImpact> findClosureImpacts(UUID accountId, boolean lockOrganizations);
    List<AccountDeletionBlocker> findBlockers(List<UUID> organizationIds);
    void begin(UUID requestId, UUID accountId, List<UUID> organizationIds, Instant capturedAt);
    List<UUID> findOrganizationIds(UUID requestId);
    void cancel(UUID requestId, Instant cancelledAt, boolean blocked);
    List<String> complete(UUID requestId, UUID accountId, Instant completedAt);
}
