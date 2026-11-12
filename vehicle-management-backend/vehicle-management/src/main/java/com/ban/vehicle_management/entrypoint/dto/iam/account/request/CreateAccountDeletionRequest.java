package com.ban.vehicle_management.entrypoint.dto.iam.account.request;

public record CreateAccountDeletionRequest(
        boolean confirmOrganizationClosure,
        boolean confirmActiveSubscriptionTerminationWithoutRefund) {
}
