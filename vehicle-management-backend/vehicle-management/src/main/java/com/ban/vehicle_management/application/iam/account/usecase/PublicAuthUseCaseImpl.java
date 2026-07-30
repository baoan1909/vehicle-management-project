package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.command.RequestPasswordResetCommand;
import com.ban.vehicle_management.application.iam.account.model.command.ResendVerificationEmailCommand;
import com.ban.vehicle_management.application.iam.account.model.result.RegisterAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.PublicAuthPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.VerificationEmailRateLimitPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.VerificationEmailResendPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PublicAuthUseCaseImpl implements PublicAuthPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicAuthUseCaseImpl.class);

    private final AccountRegistrationPortOut accountRegistrationPortOut;
    private final IdentityProviderAdminPortOut identityProviderAdminPortOut;
    private final VerificationEmailRateLimitPortOut verificationEmailRateLimitPortOut;
    private final VerificationEmailResendPolicy verificationEmailResendPolicy;
    private final PublicAuthPolicy publicAuthPolicy;
    private final NotificationPortIn notificationPortIn;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();
    private final Clock clock;

    public PublicAuthUseCaseImpl(
            AccountRegistrationPortOut accountRegistrationPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            VerificationEmailRateLimitPortOut verificationEmailRateLimitPortOut,
            VerificationEmailResendPolicy verificationEmailResendPolicy,
            PublicAuthPolicy publicAuthPolicy,
            NotificationPortIn notificationPortIn
    ) {
        this.accountRegistrationPortOut = accountRegistrationPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.verificationEmailRateLimitPortOut = verificationEmailRateLimitPortOut;
        this.verificationEmailResendPolicy = verificationEmailResendPolicy;
        this.publicAuthPolicy = publicAuthPolicy;
        this.notificationPortIn = notificationPortIn;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public RegisterAccountResult register(RegisterAccountCommand command) {
        RegisterAccountCommand normalizedRequest = publicAuthPolicy.normalizeRegisterCommand(command);
        ensureRegisterNoConflict(normalizedRequest);

        String keycloakUserId = identityProviderAdminPortOut.createUser(normalizedRequest);
        try {
            UserProfile userProfile = buildMinimalUserProfile(normalizedRequest.fullName());
            Account registeredAccount = accountRegistrationPortOut.registerAccount(
                    normalizedRequest,
                    keycloakUserId,
                    userProfile
            );
            identityProviderAdminPortOut.updateAccountIdAttribute(keycloakUserId, registeredAccount.getAccountId());
            sendVerificationEmailSafely(keycloakUserId, registeredAccount.getAccountId());
            RegisterAccountResult result = new RegisterAccountResult(
                    registeredAccount.getAccountId(),
                    registeredAccount.getStatus().name(),
                    "VERIFY_EMAIL",
                    true
            );
            notifyAccountRegistered(registeredAccount);
            return result;
        } catch (RuntimeException exception) {
            identityProviderAdminPortOut.deleteUser(keycloakUserId);
            throw exception;
        }
    }

    private UserProfile buildMinimalUserProfile(String fullName) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(UUID.randomUUID());
        userProfile.setFullName(fullName);
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfilePolicy.initialize(userProfile);
        return userProfile;
    }

    @Override
    public void resendVerificationEmail(ResendVerificationEmailCommand command) {
        String normalizedEmail = publicAuthPolicy.normalizeRequiredEmail(command.email());

        Optional<Account> accountOptional = accountRegistrationPortOut.findByEmail(normalizedEmail);
        if (accountOptional.isEmpty()) {
            return;
        }

        Account account = accountOptional.get();
        if (!AccountStatus.PENDING.equals(account.getStatus())) {
            return;
        }

        String keycloakUserId = account.getKeycloakUserId();
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            return;
        }

        if (identityProviderAdminPortOut.isEmailVerified(keycloakUserId)) {
            return;
        }

        Instant requestedAt = Instant.now(clock);
        VerificationEmailRateLimitPortOut.VerificationEmailRateLimitSnapshot snapshot =
                verificationEmailRateLimitPortOut.loadSnapshot(
                        normalizedEmail,
                        verificationEmailResendPolicy.windowStartAt(requestedAt)
                );
        VerificationEmailResendPolicy.VerificationEmailResendDecision decision =
                verificationEmailResendPolicy.evaluate(requestedAt, snapshot);
        if (!decision.allowed()) {
            throw new TooManyRequestsException(
                    "Bạn đã yêu cầu gửi lại email quá nhiều lần. Vui lòng thử lại sau "
                            + decision.retryAfterSeconds()
                            + " giây."
            );
        }

        verificationEmailRateLimitPortOut.saveAttempt(normalizedEmail, requestedAt);
        identityProviderAdminPortOut.sendVerifyEmail(keycloakUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requestPasswordReset(RequestPasswordResetCommand command) {
        String normalizedEmail = publicAuthPolicy.normalizeRequiredEmail(command.email());

        accountRegistrationPortOut.findKeycloakUserIdByEmail(normalizedEmail)
                .ifPresent(identityProviderAdminPortOut::sendUpdatePasswordEmail);
    }

    private void ensureRegisterNoConflict(RegisterAccountCommand command) {
        if (accountRegistrationPortOut.existsByUsername(command.username())) {
            throw new ConflictException("Tên đăng nhập đã tồn tại.");
        }

        if (accountRegistrationPortOut.existsByEmail(command.email())) {
            throw new ConflictException("Email đã tồn tại.");
        }
    }

    private void sendVerificationEmailSafely(String keycloakUserId, UUID accountId) {
        try {
            identityProviderAdminPortOut.sendVerifyEmail(keycloakUserId);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Account {} created but verification email could not be sent. The user can request resend later.",
                    accountId,
                    exception
            );
        }
    }

    private void notifyAccountRegistered(Account account) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                account.getAccountId(),
                "Đăng ký tài khoản thành công",
                "Tài khoản của bạn đã được tạo. Vui lòng xác thực email và hoàn tất hồ sơ để gửi duyệt.",
                "iam",
                "accounts",
                account.getAccountId()
        ));
    }
}
