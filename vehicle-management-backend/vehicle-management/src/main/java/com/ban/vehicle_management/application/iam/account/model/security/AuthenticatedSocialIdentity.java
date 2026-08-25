package com.ban.vehicle_management.application.iam.account.model.security;

public record AuthenticatedSocialIdentity(
        String keycloakUserId,
        String providerAlias,
        String email,
        boolean emailVerified,
        String preferredUsername,
        String fullName
) {
}
