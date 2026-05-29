package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.RegisterAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPort;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RegisterAccountUseCaseImpl implements RegisterAccountPortIn {
    private final AccountRegistrationPortOut accountRegistrationPortOut;
    private final IdentityProviderAdminPort identityProviderAdminPort;

    public RegisterAccountUseCaseImpl(
            AccountRegistrationPortOut accountRegistrationPortOut,
            IdentityProviderAdminPort identityProviderAdminPort
    ) {
        this.accountRegistrationPortOut = accountRegistrationPortOut;
        this.identityProviderAdminPort = identityProviderAdminPort;
    }

    @Override
    public RegisterAccountResult register(RegisterAccountCommand command) {
        RegisterAccountCommand normalizedRequest = normalize(command);
        ensureNoConflict(normalizedRequest);

        String keycloakUserId = identityProviderAdminPort.createUser(normalizedRequest);
        try {
            Account registeredAccount = accountRegistrationPortOut.registerAccount(normalizedRequest, keycloakUserId);
            identityProviderAdminPort.updateAccountIdAttribute(keycloakUserId, registeredAccount.getAccountId());
            identityProviderAdminPort.sendVerifyEmail(keycloakUserId);
            return new RegisterAccountResult(
                    registeredAccount.getAccountId(),
                    registeredAccount.getStatus().name(),
                    "VERIFY_EMAIL",
                    true
            );
        } catch (RuntimeException exception) {
            identityProviderAdminPort.deleteUser(keycloakUserId);
            throw exception;
        }
    }

    private RegisterAccountCommand normalize(RegisterAccountCommand command) {
        String username = TextValidationUtils.normalizeRequiredText(command.username(), "username", 100);
        String email = TextValidationUtils.normalizeRequiredText(command.email(), "email", 255)
                .toLowerCase(Locale.ROOT);
        String password = normalizePassword(command.password());

        return new RegisterAccountCommand(
                username,
                email,
                password
        );
    }

    private String normalizePassword(String password) {
        String normalizedPassword = TextValidationUtils.normalizeRequiredText(password, "password", 255);
        if (normalizedPassword.length() < 8) {
            throw new BadRequestException("password must be at least 8 characters");
        }
        return normalizedPassword;
    }

    private void ensureNoConflict(RegisterAccountCommand command) {
        if (accountRegistrationPortOut.existsByUsername(command.username())) {
            throw new ConflictException("Username already exists");
        }

        if (accountRegistrationPortOut.existsByEmail(command.email())) {
            throw new ConflictException("Email already exists");
        }
    }
}
