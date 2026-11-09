package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;

public interface AccountDeletionPortIn {
    AccountDeletionRequest requestCurrentAccountDeletion();
    Optional<AccountDeletionRequest> getCurrentRequest();
    AccountDeletionRequest cancelCurrentRequest(String reason);
    Optional<AccountDeletionRequest> cancelOnFreshLogin(UUID accountId, Instant authenticatedAt);
    AccountDeletionView requestCurrentAccountDeletionView();
    Optional<AccountDeletionView> getCurrentRequestView();
    AccountDeletionView cancelCurrentRequestView(String reason);
}
