package com.ban.vehicle_management.application.iam.account.model.security;

public record FederatedIdentityInfo(
        String identityProvider,
        String userId,
        String userName
) {
}
