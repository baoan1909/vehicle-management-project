package com.ban.vehicle_management.application.iam.account.port.in;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import com.ban.vehicle_management.application.iam.account.model.result.AccountDeletionView;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionImpact;

public interface AccountDeletionPortIn {
    AccountDeletionRequest requestCurrentAccountDeletion();
    Optional<AccountDeletionRequest> getCurrentRequest();
    AccountDeletionRequest cancelCurrentRequest(String reason);
    Optional<AccountDeletionRequest> cancelOnFreshLogin(UUID accountId, Instant authenticatedAt);
    AccountDeletionView requestCurrentAccountDeletionView();
    AccountDeletionView requestCurrentAccountDeletionView(boolean confirmOrganizationClosure);
    AccountDeletionView requestCurrentAccountDeletionView(boolean confirmOrganizationClosure,
            boolean confirmActiveSubscriptionTerminationWithoutRefund);
    AccountDeletionImpact getCurrentAccountDeletionImpact();
    Optional<AccountDeletionView> getCurrentRequestView();
    default AccountDeletionView getCurrentRequestViewOrNull() {
        return getCurrentRequestView().orElse(null);
    }
    AccountDeletionView cancelCurrentRequestView(String reason);
}
