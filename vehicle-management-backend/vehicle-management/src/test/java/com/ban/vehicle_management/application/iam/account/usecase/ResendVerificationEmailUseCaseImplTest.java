package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.ResendVerificationEmailCommand;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.VerificationEmailResendPolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailUseCaseImplTest {

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
    void shouldIgnoreWhenAccountDoesNotExist() {
        when(publicAuthPolicy.normalizeRequiredEmail("user@example.com")).thenReturn("user@example.com");
        when(accountRegistrationPortOut.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> publicAuthUseCase.resendVerificationEmail(
                new ResendVerificationEmailCommand("user@example.com")
        ));

        verify(identityProviderAdminPortOut, never()).sendVerifyEmail(any());
        verify(verificationEmailRateLimitPortOut, never()).loadSnapshot(any(), any(Instant.class));
    }

    @Test
    void shouldIgnoreWhenAccountIsNotPending() {
        when(publicAuthPolicy.normalizeRequiredEmail("user@example.com")).thenReturn("user@example.com");
        Account account = new Account();
        account.setStatus(AccountStatus.ACTIVE);
        account.setKeycloakUserId("kc-user-id");
        when(accountRegistrationPortOut.findByEmail("user@example.com")).thenReturn(Optional.of(account));

        publicAuthUseCase.resendVerificationEmail(new ResendVerificationEmailCommand("user@example.com"));

        verify(identityProviderAdminPortOut, never()).isEmailVerified(any());
        verify(verificationEmailRateLimitPortOut, never()).loadSnapshot(any(), any(Instant.class));
    }

    @Test
    void shouldIgnoreWhenKeycloakEmailAlreadyVerified() {
        when(publicAuthPolicy.normalizeRequiredEmail("user@example.com")).thenReturn("user@example.com");
        Account account = new Account();
        account.setStatus(AccountStatus.PENDING);
        account.setKeycloakUserId("kc-user-id");
        when(accountRegistrationPortOut.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(identityProviderAdminPortOut.isEmailVerified("kc-user-id")).thenReturn(true);

        publicAuthUseCase.resendVerificationEmail(new ResendVerificationEmailCommand("user@example.com"));

        verify(verificationEmailRateLimitPortOut, never()).loadSnapshot(any(), any(Instant.class));
        verify(identityProviderAdminPortOut, never()).sendVerifyEmail(any());
    }

    @Test
    void shouldThrowTooManyRequestsWhenRateLimitExceeded() {
        when(publicAuthPolicy.normalizeRequiredEmail("user@example.com")).thenReturn("user@example.com");
        Account account = new Account();
        account.setStatus(AccountStatus.PENDING);
        account.setKeycloakUserId("kc-user-id");
        Instant now = Instant.parse("2026-05-30T00:00:00Z");
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                new VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot(
                        Optional.of(now.minusSeconds(10)),
                        Optional.of(now.minusSeconds(3500)),
                        5
                );

        when(accountRegistrationPortOut.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(identityProviderAdminPortOut.isEmailVerified("kc-user-id")).thenReturn(false);
        when(verificationEmailResendPolicy.windowStartAt(any(Instant.class))).thenReturn(now.minusSeconds(3600));
        when(verificationEmailRateLimitPortOut.loadSnapshot(eq("user@example.com"), any(Instant.class))).thenReturn(snapshot);
        when(verificationEmailResendPolicy.evaluate(any(Instant.class), eq(snapshot)))
                .thenReturn(new VerificationEmailResendPolicy.VerificationEmailResendDecision(false, 12));

        assertThrows(
                TooManyRequestsException.class,
                () -> publicAuthUseCase.resendVerificationEmail(new ResendVerificationEmailCommand("user@example.com"))
        );

        verify(verificationEmailRateLimitPortOut, never()).saveAttempt(any(), any(Instant.class));
        verify(identityProviderAdminPortOut, never()).sendVerifyEmail(any());
    }

    @Test
    void shouldSendVerificationEmailWhenAllChecksPass() {
        when(publicAuthPolicy.normalizeRequiredEmail("user@example.com")).thenReturn("user@example.com");
        Account account = new Account();
        account.setStatus(AccountStatus.PENDING);
        account.setKeycloakUserId("kc-user-id");
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                new VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot(
                        Optional.empty(),
                        Optional.empty(),
                        0
                );

        when(accountRegistrationPortOut.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(identityProviderAdminPortOut.isEmailVerified("kc-user-id")).thenReturn(false);
        when(verificationEmailResendPolicy.windowStartAt(any(Instant.class))).thenReturn(Instant.parse("2026-05-29T23:00:00Z"));
        when(verificationEmailRateLimitPortOut.loadSnapshot(eq("user@example.com"), any(Instant.class))).thenReturn(snapshot);
        when(verificationEmailResendPolicy.evaluate(any(Instant.class), eq(snapshot)))
                .thenReturn(new VerificationEmailResendPolicy.VerificationEmailResendDecision(true, 0));

        publicAuthUseCase.resendVerificationEmail(new ResendVerificationEmailCommand("user@example.com"));

        verify(verificationEmailRateLimitPortOut).saveAttempt(eq("user@example.com"), any(Instant.class));
        verify(identityProviderAdminPortOut).sendVerifyEmail("kc-user-id");
    }
}
