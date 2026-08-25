package com.ban.vehicle_management.application.iam.account.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.model.result.SocialAccountBootstrapResult;
import com.ban.vehicle_management.application.iam.account.model.security.AuthenticatedSocialIdentity;
import com.ban.vehicle_management.application.iam.account.model.security.FederatedIdentityInfo;
import com.ban.vehicle_management.application.iam.account.port.in.AuthenticatedSocialIdentityPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SocialAccountRegistrationPortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.AccountIdentity;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.account.policy.SocialAccountPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.SocialIdentityProvider;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialAccountBootstrapUseCaseImplTest {

    @Mock
    private AuthenticatedSocialIdentityPortIn authenticatedSocialIdentityPortIn;

    @Mock
    private IdentityProviderAdminPortOut identityProviderAdminPortOut;

    @Mock
    private SocialAccountRegistrationPortOut socialAccountRegistrationPortOut;

    private SocialAccountBootstrapUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SocialAccountBootstrapUseCaseImpl(
                authenticatedSocialIdentityPortIn,
                identityProviderAdminPortOut,
                socialAccountRegistrationPortOut,
                new SocialAccountPolicy(new PublicAuthPolicy())
        );
    }

    @Test
    void shouldCreatePendingCustomerWithImmutableGoogleSubject() {
        stubAuthenticatedGoogleIdentity();
        when(socialAccountRegistrationPortOut.findAccountByKeycloakUserId("kc-user-1"))
                .thenReturn(Optional.empty());
        when(socialAccountRegistrationPortOut.findIdentity(SocialIdentityProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(socialAccountRegistrationPortOut.existsByEmail("customer@example.com")).thenReturn(false);
        when(socialAccountRegistrationPortOut.registerCustomer(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SocialAccountBootstrapResult result = useCase.bootstrap();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        ArgumentCaptor<AccountIdentity> identityCaptor = ArgumentCaptor.forClass(AccountIdentity.class);
        verify(socialAccountRegistrationPortOut).registerCustomer(
                accountCaptor.capture(),
                profileCaptor.capture(),
                identityCaptor.capture()
        );

        assertTrue(result.created());
        assertEquals("CUSTOMER", result.roleCode());
        assertEquals(AccountStatus.PENDING, accountCaptor.getValue().getStatus());
        assertEquals("customer@example.com", accountCaptor.getValue().getEmail());
        assertTrue(accountCaptor.getValue().getUsername().matches("customerone_[a-z0-9]{6}"));
        assertEquals("Customer One", profileCaptor.getValue().getFullName());
        assertEquals("google-sub-123", identityCaptor.getValue().getProviderSubject());
        assertEquals(accountCaptor.getValue().getAccountId(), identityCaptor.getValue().getAccountId());
        verify(identityProviderAdminPortOut).updateAccountIdAttribute(
                "kc-user-1",
                accountCaptor.getValue().getAccountId()
        );
    }

    @Test
    void shouldReturnExistingAccountOnlyWhenMatchingSocialIdentityExists() {
        stubAuthenticatedGoogleIdentity();
        UUID accountId = UUID.randomUUID();
        Account account = account(accountId);
        AccountIdentity identity = identity(accountId, "google-sub-123");
        when(socialAccountRegistrationPortOut.findAccountByKeycloakUserId("kc-user-1"))
                .thenReturn(Optional.of(account));
        when(socialAccountRegistrationPortOut.findIdentity(accountId, SocialIdentityProvider.GOOGLE))
                .thenReturn(Optional.of(identity));

        SocialAccountBootstrapResult result = useCase.bootstrap();

        assertFalse(result.created());
        assertEquals(accountId, result.accountId());
        verify(socialAccountRegistrationPortOut, never()).registerCustomer(any(), any(), any());
    }

    @Test
    void shouldRejectPasswordAccountEvenWhenKeycloakSubjectMatches() {
        stubAuthenticatedGoogleIdentity();
        UUID accountId = UUID.randomUUID();
        when(socialAccountRegistrationPortOut.findAccountByKeycloakUserId("kc-user-1"))
                .thenReturn(Optional.of(account(accountId)));
        when(socialAccountRegistrationPortOut.findIdentity(accountId, SocialIdentityProvider.GOOGLE))
                .thenReturn(Optional.empty());

        ConflictException exception = assertThrows(ConflictException.class, useCase::bootstrap);

        assertTrue(exception.getMessage().contains("không cho phép liên kết"));
        verify(socialAccountRegistrationPortOut, never()).registerCustomer(any(), any(), any());
    }

    @Test
    void shouldRejectExistingEmailInsteadOfLinkingAccount() {
        stubAuthenticatedGoogleIdentity();
        when(socialAccountRegistrationPortOut.findAccountByKeycloakUserId("kc-user-1"))
                .thenReturn(Optional.empty());
        when(socialAccountRegistrationPortOut.findIdentity(SocialIdentityProvider.GOOGLE, "google-sub-123"))
                .thenReturn(Optional.empty());
        when(socialAccountRegistrationPortOut.existsByEmail("customer@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, useCase::bootstrap);

        verify(socialAccountRegistrationPortOut, never()).registerCustomer(any(), any(), any());
    }

    @Test
    void shouldRejectUnverifiedGoogleEmailBeforeReadingFederatedIdentity() {
        AuthenticatedSocialIdentity identity = new AuthenticatedSocialIdentity(
                "kc-user-1",
                "google",
                "customer@example.com",
                false,
                "customer@example.com",
                "Customer One"
        );
        when(authenticatedSocialIdentityPortIn.getAuthenticatedIdentityOrThrow()).thenReturn(identity);

        assertThrows(BadRequestException.class, useCase::bootstrap);

        verify(identityProviderAdminPortOut, never()).findFederatedIdentities(any());
    }

    private void stubAuthenticatedGoogleIdentity() {
        AuthenticatedSocialIdentity identity = new AuthenticatedSocialIdentity(
                "kc-user-1",
                "google",
                "Customer@Example.com",
                true,
                "customer@example.com",
                "Customer One"
        );
        when(authenticatedSocialIdentityPortIn.getAuthenticatedIdentityOrThrow()).thenReturn(identity);
        when(identityProviderAdminPortOut.findFederatedIdentities("kc-user-1"))
                .thenReturn(List.of(new FederatedIdentityInfo("google", "google-sub-123", "customer@example.com")));
    }

    private Account account(UUID accountId) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setKeycloakUserId("kc-user-1");
        account.setStatus(AccountStatus.PENDING);
        return account;
    }

    private AccountIdentity identity(UUID accountId, String providerSubject) {
        AccountIdentity identity = new AccountIdentity();
        identity.setAccountId(accountId);
        identity.setProvider(SocialIdentityProvider.GOOGLE);
        identity.setProviderSubject(providerSubject);
        return identity;
    }
}
