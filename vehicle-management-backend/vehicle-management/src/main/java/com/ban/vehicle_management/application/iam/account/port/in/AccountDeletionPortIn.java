package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountDeletionPortIn {
    AccountDeletionRequest requestCurrentAccountDeletion();
    Optional<AccountDeletionRequest> getCurrentRequest();
    AccountDeletionRequest cancelCurrentRequest(String reason);
    Optional<AccountDeletionRequest> cancelOnFreshLogin(UUID accountId, Instant authenticatedAt);
}
