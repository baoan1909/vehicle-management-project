package com.ban.vehicle_management.application.iam.account.port.out;

import com.ban.vehicle_management.application.iam.account.model.command.CreateProvisionedAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;

import java.util.UUID;

public interface IdentityProviderAdminPortOut {

    String createUser(RegisterAccountCommand command);

    String createProvisionedAccountUser(CreateProvisionedAccountCommand command);

    void updateAccountIdAttribute(String keycloakUserId, UUID accountId);

    void updateUserEnabled(String keycloakUserId, boolean enabled);

    void sendVerifyEmail(String keycloakUserId);

    void sendUpdatePasswordEmail(String keycloakUserId);

    boolean isEmailVerified(String keycloakUserId);

    void deleteUser(String keycloakUserId);
}
