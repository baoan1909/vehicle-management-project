package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RequestPasswordResetCommand;
import com.ban.vehicle_management.application.iam.account.port.in.RequestPasswordResetPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPort;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RequestPasswordResetUseCaseImpl implements RequestPasswordResetPortIn {

    private final AccountRegistrationPortOut accountRegistrationPortOut;
    private final IdentityProviderAdminPort identityProviderAdminPort;

    public RequestPasswordResetUseCaseImpl(
            AccountRegistrationPortOut accountRegistrationPortOut,
            IdentityProviderAdminPort identityProviderAdminPort
    ) {
        this.accountRegistrationPortOut = accountRegistrationPortOut;
        this.identityProviderAdminPort = identityProviderAdminPort;
    }

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        String normalizedEmail = TextValidationUtils.normalizeRequiredText(command.email(), "email", 255)
                .toLowerCase(Locale.ROOT);

        accountRegistrationPortOut.findKeycloakUserIdByEmail(normalizedEmail)
                .ifPresent(identityProviderAdminPort::sendUpdatePasswordEmail);
    }
}
