package com.ban.vehicle_management.application.iam.account.model.result;

import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;

public record AccountDeletionView(AccountDeletionRequest request, boolean cancellable) {
}
