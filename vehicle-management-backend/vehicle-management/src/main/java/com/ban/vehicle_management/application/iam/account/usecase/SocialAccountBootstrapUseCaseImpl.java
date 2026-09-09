package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.iam.account.model.result.SocialAccountBootstrapResult;
import com.ban.vehicle_management.application.iam.account.model.security.AuthenticatedSocialIdentity;
import com.ban.vehicle_management.application.iam.account.model.security.FederatedIdentityInfo;
import com.ban.vehicle_management.application.iam.account.port.in.AuthenticatedSocialIdentityPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.SocialAccountBootstrapPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.domain.iam.account.policy.SocialAccountPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialAccountBootstrapUseCaseImpl implements SocialAccountBootstrapPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocialAccountBootstrapUseCaseImpl.class);
    private static final String CUSTOMER_ROLE_CODE = "CUSTOMER";

    private final AuthenticatedSocialIdentityPortIn authenticatedSocialIdentityPortIn;
    private final IdentityProviderAdminPortOut identityProviderAdminPortOut;
    private final SocialAccountRegistrationPortOut socialAccountRegistrationPortOut;
    private final SocialAccountPolicy socialAccountPolicy;
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public SocialAccountBootstrapUseCaseImpl(
            AuthenticatedSocialIdentityPortIn authenticatedSocialIdentityPortIn,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            SocialAccountRegistrationPortOut socialAccountRegistrationPortOut,
            SocialAccountPolicy socialAccountPolicy
    ) {
        this.authenticatedSocialIdentityPortIn = authenticatedSocialIdentityPortIn;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.socialAccountRegistrationPortOut = socialAccountRegistrationPortOut;
        this.socialAccountPolicy = socialAccountPolicy;
    }

    @Override
    @Transactional
    public SocialAccountBootstrapResult bootstrap() {
        AuthenticatedSocialIdentity authenticatedIdentity =
                authenticatedSocialIdentityPortIn.getAuthenticatedIdentityOrThrow();
        SocialIdentityProvider provider = socialAccountPolicy.requireEnabledProvider(
                authenticatedIdentity.providerAlias()
        );
        String email = socialAccountPolicy.requireVerifiedEmail(
                authenticatedIdentity.email(),
                authenticatedIdentity.emailVerified()
        );
        FederatedIdentityInfo federatedIdentity = requireFederatedIdentity(
                authenticatedIdentity.keycloakUserId(),
                authenticatedIdentity.providerAlias()
        );
        String providerSubject = socialAccountPolicy.normalizeProviderSubject(federatedIdentity.userId());
        socialAccountRegistrationPortOut.lockRegistration(provider, providerSubject, email);

        Account accountForKeycloakUser = socialAccountRegistrationPortOut
                .findAccountByKeycloakUserId(authenticatedIdentity.keycloakUserId())
                .orElse(null);
        if (accountForKeycloakUser != null) {
            return requireExistingSocialAccount(accountForKeycloakUser, provider, providerSubject);
        }

        if (socialAccountRegistrationPortOut.findIdentity(provider, providerSubject).isPresent()) {
            throw new ConflictException("Danh tính Google này đã thuộc một tài khoản khác.");
        }
        if (socialAccountRegistrationPortOut.existsByEmail(email)) {
            throw passwordAccountConflict();
        }

        UUID accountId = UUID.randomUUID();
        UserProfile userProfile = buildMinimalProfile(authenticatedIdentity.fullName(), email);
        Account account = buildPendingCustomerAccount(
                accountId,
                userProfile.getUserProfileId(),
                authenticatedIdentity,
                provider,
                email,
                userProfile.getFullName()
        );
        AccountIdentity accountIdentity = buildAccountIdentity(
                accountId,
                provider,
                providerSubject,
                federatedIdentity.userName(),
                email
        );

        Account registeredAccount = socialAccountRegistrationPortOut.registerCustomer(
                account,
                userProfile,
                accountIdentity
        );
        syncAccountIdSafely(authenticatedIdentity.keycloakUserId(), registeredAccount.getAccountId());
        return toResult(registeredAccount, provider, true);
    }

    private SocialAccountBootstrapResult requireExistingSocialAccount(
            Account account,
            SocialIdentityProvider provider,
            String providerSubject
    ) {
        AccountIdentity accountIdentity = socialAccountRegistrationPortOut
                .findIdentity(account.getAccountId(), provider)
                .orElseThrow(this::passwordAccountConflict);
        if (!Objects.equals(accountIdentity.getProviderSubject(), providerSubject)) {
            throw new ConflictException("Danh tính Google không khớp với tài khoản đã đăng ký.");
        }
        syncAccountIdSafely(account.getKeycloakUserId(), account.getAccountId());
        return toResult(account, provider, false);
    }

    private FederatedIdentityInfo requireFederatedIdentity(String keycloakUserId, String providerAlias) {
        return identityProviderAdminPortOut.findFederatedIdentities(keycloakUserId).stream()
                .filter(identity -> identity.identityProvider() != null)
                .filter(identity -> identity.identityProvider().equalsIgnoreCase(providerAlias))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy danh tính Google đã xác thực trong Keycloak."));
    }

    private UserProfile buildMinimalProfile(String fullName, String email) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(UUID.randomUUID());
        userProfile.setFullName(socialAccountPolicy.resolveFullName(fullName, email));
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfilePolicy.initialize(userProfile);
        return userProfile;
    }

    private Account buildPendingCustomerAccount(
            UUID accountId,
            UUID userProfileId,
            AuthenticatedSocialIdentity authenticatedIdentity,
            SocialIdentityProvider provider,
            String email,
            String fullName
    ) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setUserProfileId(userProfileId);
        account.setKeycloakUserId(authenticatedIdentity.keycloakUserId());
        account.setUsername(socialAccountPolicy.buildUsername(
                provider,
                fullName,
                authenticatedIdentity.keycloakUserId()
        ));
        account.setEmail(email);
        account.setStatus(AccountStatus.PENDING);
        account.setFailedLoginCount(0);
        return account;
    }

    private AccountIdentity buildAccountIdentity(
            UUID accountId,
            SocialIdentityProvider provider,
            String providerSubject,
            String providerUsername,
            String email
    ) {
        AccountIdentity accountIdentity = new AccountIdentity();
        accountIdentity.setAccountIdentityId(UUID.randomUUID());
        accountIdentity.setAccountId(accountId);
        accountIdentity.setProvider(provider);
        accountIdentity.setProviderSubject(providerSubject);
        accountIdentity.setProviderUsername(socialAccountPolicy.normalizeProviderUsername(providerUsername));
        accountIdentity.setProviderEmail(email);
        return accountIdentity;
    }

    private void syncAccountIdSafely(String keycloakUserId, UUID accountId) {
        try {
            identityProviderAdminPortOut.updateAccountIdAttribute(keycloakUserId, accountId);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Social account {} exists but account_id could not be synchronized to Keycloak user {}",
                    accountId,
                    keycloakUserId,
                    exception
            );
        }
    }

    private ConflictException passwordAccountConflict() {
        return new ConflictException(
                "Email này đã thuộc tài khoản dùng mật khẩu. Hệ thống không cho phép liên kết với Google; vui lòng chọn tài khoản Google khác."
        );
    }

    private SocialAccountBootstrapResult toResult(
            Account account,
            SocialIdentityProvider provider,
            boolean created
    ) {
        return new SocialAccountBootstrapResult(
                account.getAccountId(),
                account.getStatus().name(),
                CUSTOMER_ROLE_CODE,
                provider.name(),
                created
        );
    }
}
