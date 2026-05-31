package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.policy.VerificationEmailResendPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterAccountUseCaseImplTest {

    @Mock
    private AccountRegistrationPortOut accountRegistrationPortOut;

    @Mock
    private IdentityProviderAdminPortOut identityProviderAdminPortOut;

    @Mock
    private VerificationEmailRateLimitPortOut verificationEmailRateLimitPortOut;

    @Mock
    private VerificationEmailResendPolicy verificationEmailResendPolicy;

    @Mock
    private PublicAuthPolicy publicAuthPolicy;

    @InjectMocks
    private PublicAuthUseCaseImpl publicAuthUseCase;

    @Test
    void shouldRegisterSuccessfullyWhenVerificationEmailFails() {
        RegisterAccountCommand command = new RegisterAccountCommand("new-user", "user@example.com", "12345678");
        when(publicAuthPolicy.normalizeRegisterCommand(command)).thenReturn(command);
        String keycloakUserId = "kc-user-id";
        UUID accountId = UUID.randomUUID();
        Account registeredAccount = buildAccount(accountId, keycloakUserId);

        when(accountRegistrationPortOut.existsByUsername("new-user")).thenReturn(false);
        when(accountRegistrationPortOut.existsByEmail("user@example.com")).thenReturn(false);
        when(identityProviderAdminPortOut.createUser(command)).thenReturn(keycloakUserId);
        when(accountRegistrationPortOut.registerAccount(command, keycloakUserId)).thenReturn(registeredAccount);
        org.mockito.Mockito.doThrow(new BadRequestException("smtp down"))
                .when(identityProviderAdminPortOut).sendVerifyEmail(keycloakUserId);

        RegisterAccountResult result = publicAuthUseCase.register(command);

        assertEquals(accountId, result.accountId());
        assertEquals(AccountStatus.PENDING.name(), result.accountStatus());
        verify(identityProviderAdminPortOut, never()).deleteUser(keycloakUserId);
    }

    @Test
    void shouldRollbackKeycloakUserWhenLocalRegistrationFails() {
        RegisterAccountCommand command = new RegisterAccountCommand("new-user", "user@example.com", "12345678");
        when(publicAuthPolicy.normalizeRegisterCommand(command)).thenReturn(command);
        String keycloakUserId = "kc-user-id";

        when(accountRegistrationPortOut.existsByUsername("new-user")).thenReturn(false);
        when(accountRegistrationPortOut.existsByEmail("user@example.com")).thenReturn(false);
        when(identityProviderAdminPortOut.createUser(command)).thenReturn(keycloakUserId);
        when(accountRegistrationPortOut.registerAccount(command, keycloakUserId))
                .thenThrow(new RuntimeException("db failed"));

        assertThrows(RuntimeException.class, () -> publicAuthUseCase.register(command));

        verify(identityProviderAdminPortOut).deleteUser(keycloakUserId);
    }

    @Test
    void shouldAttemptToSendVerificationEmailWhenRegistrationSucceeds() {
        RegisterAccountCommand command = new RegisterAccountCommand("new-user", "user@example.com", "12345678");
        when(publicAuthPolicy.normalizeRegisterCommand(command)).thenReturn(command);
        String keycloakUserId = "kc-user-id";
        UUID accountId = UUID.randomUUID();
        Account registeredAccount = buildAccount(accountId, keycloakUserId);

        when(accountRegistrationPortOut.existsByUsername("new-user")).thenReturn(false);
        when(accountRegistrationPortOut.existsByEmail("user@example.com")).thenReturn(false);
        when(identityProviderAdminPortOut.createUser(command)).thenReturn(keycloakUserId);
        when(accountRegistrationPortOut.registerAccount(command, keycloakUserId)).thenReturn(registeredAccount);

        publicAuthUseCase.register(command);

        verify(identityProviderAdminPortOut).sendVerifyEmail(keycloakUserId);
    }

    private Account buildAccount(UUID accountId, String keycloakUserId) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setKeycloakUserId(keycloakUserId);
        account.setStatus(AccountStatus.PENDING);
        return account;
    }
}
