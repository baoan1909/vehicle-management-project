package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CompletePartnerProfileCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.operations.approvalrequest.usecase.OnboardingApprovalPolicyEvaluator;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerApprovalValidator;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PartnerRegistrationUseCaseImplTest {

    @Mock private PartnerRegistrationPortOut partnerRegistrationPortOut;
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private AccountRegistrationPortOut accountRegistrationPortOut;
    @Mock private IdentityProviderAdminPortOut identityProviderAdminPortOut;
    @Mock private ProvisionedAccountPortOut provisionedAccountPortOut;
    @Mock private SystemAccountIdPortOut systemAccountIdPortOut;
    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private PartnerCatalogSeedPortOut partnerCatalogSeedPortOut;
    @Mock private OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator;
    @Mock private VehicleMailService vehicleMailService;
    @Mock private RequiresNewNotificationSender notificationSender;
    @Mock private com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut userProfilePortOut;
    @Mock private com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn userProfileAvatarPortIn;
    @Mock private com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy userProfilePolicy;
    @Mock private com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy organizationPolicy;
    @Mock private com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerApprovalValidator partnerApprovalValidator;

    private PartnerRegistrationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PartnerRegistrationUseCaseImpl(
                partnerRegistrationPortOut,
                currentAccountPortIn,
                accountRegistrationPortOut,
                identityProviderAdminPortOut,
                provisionedAccountPortOut,
                systemAccountIdPortOut,
                organizationPortOut,
                partnerCatalogSeedPortOut,
                onboardingApprovalPolicyEvaluator,
                new PublicAuthPolicy(),
                vehicleMailService,
                notificationSender,
                userProfilePortOut,
                userProfileAvatarPortIn,
                userProfilePolicy,
                organizationPolicy,
                partnerApprovalValidator
        );
    }

    @Test
    void shouldCreateKeycloakUserPendingPartnerAdminAndLinkedApprovalWithoutPassword() {
        UUID accountId = UUID.randomUUID();
        when(identityProviderAdminPortOut.createUser(any())).thenReturn("keycloak-user-id");
        when(accountRegistrationPortOut.registerPendingAccount(
                any(), eq("keycloak-user-id"), any(), eq(AdminProvisionableAccountRoleCode.PARTNER_ADMIN)
        )).thenReturn(pendingAccount(accountId));

        PartnerRegistrationSubmissionResult result = useCase.submitRegistration(validCommand());

        ArgumentCaptor<ApprovalRequest> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(partnerRegistrationPortOut).save(approvalCaptor.capture());
        ApprovalRequest approval = approvalCaptor.getValue();
        assertEquals(accountId, approval.getRequestedBy());
        assertEquals(accountId, approval.getTargetId());
        assertEquals(ApprovalRequestStatus.PENDING, approval.getStatus());
        assertEquals("PARTNER_ABC", approval.getRequestData().get("organizationCode"));
        assertEquals("Bãi xe ABC", approval.getRequestData().get("organizationName"));
        assertEquals(2, approval.getRequestData().size());
        assertFalse(approval.getRequestData().containsKey("password"));
        assertFalse(approval.getRequestData().containsKey("fullName"));
        assertFalse(approval.getRequestData().containsKey("username"));
        assertFalse(approval.getRequestData().containsKey("email"));
        assertFalse(approval.getRequestData().containsKey("phoneNumber"));
        assertFalse(approval.getRequestData().containsKey("representativeName"));
        assertFalse(approval.getRequestData().containsKey("representativePhoneNumber"));
        assertFalse(approval.getRequestData().containsKey("address"));
        assertFalse(approval.getRequestData().containsKey("expectedParkingLotCount"));
        assertFalse(approval.getRequestData().containsKey("parkingOperationDescription"));
        assertEquals(AccountStatus.PENDING, result.accountStatus());
        assertEquals("VERIFY_EMAIL", result.nextAction());
        verify(identityProviderAdminPortOut).updateAccountIdAttribute("keycloak-user-id", accountId);
        verify(identityProviderAdminPortOut).sendVerifyEmail("keycloak-user-id");
        verify(notificationSender).send(argThat(command ->
                accountId.equals(command.accountId())
                        && NotificationType.ACCOUNT_REGISTERED.equals(command.notificationType())
        ));
    }

    @Test
    void shouldNotifyApplicantAndReviewersWhenCompletedProfileRemainsPending() {
        UUID applicantId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);
        UserProfile userProfile = completeUserProfile(userProfileId);
        VietnamAddress personalAddress = legacyAddress("25 Bùi Xuân Phái");
        VietnamAddress organizationAddress = legacyAddress("01 Võ Văn Ngân");

        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(applicantId);
        when(partnerRegistrationPortOut.findLatestByApplicantAccountId(applicantId))
                .thenReturn(Optional.of(approval));
        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));
        when(provisionedAccountPortOut.findProvisionedAccountById(applicantId))
                .thenReturn(Optional.of(pendingPartnerApplicant(applicantId, userProfileId)));
        when(identityProviderAdminPortOut.isEmailVerified("partner-keycloak-id")).thenReturn(true);
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        useCase.completeMyProfile(new CompletePartnerProfileCommand(
                "Nguyễn Văn A",
                LocalDate.of(1990, 1, 1),
                "Nam",
                "0901234567",
                "123456789012",
                personalAddress,
                "PARTNER_ABC",
                "Bãi xe ABC",
                organizationAddress
        ));

        verify(notificationSender).send(argThat(command ->
                applicantId.equals(command.accountId())
                        && NotificationType.ACCOUNT_PROFILE_SUBMITTED.equals(command.notificationType())
                        && approval.getApprovalRequestId().equals(command.relatedId())
        ));
        verify(notificationSender).sendBroadcast(argThat(command ->
                NotificationType.ACCOUNT_PROFILE_SUBMITTED.equals(command.notificationType())
                        && approval.getApprovalRequestId().equals(command.relatedId())
                        && command.recipientCriteria().requiredAnyPermissionCodes().contains("ORGANIZATION_CREATE_ALL")
        ));
    }

    @Test
    void shouldDeleteKeycloakUserWhenDatabaseRegistrationFails() {
        when(identityProviderAdminPortOut.createUser(any())).thenReturn("keycloak-user-id");
        when(accountRegistrationPortOut.registerPendingAccount(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> useCase.submitRegistration(validCommand()));

        verify(identityProviderAdminPortOut).deleteUser("keycloak-user-id");
        verify(partnerRegistrationPortOut, never()).save(any());
    }

    @Test
    void shouldKeepRegistrationWhenVerificationEmailFails() {
        UUID accountId = UUID.randomUUID();
        when(identityProviderAdminPortOut.createUser(any())).thenReturn("keycloak-user-id");
        when(accountRegistrationPortOut.registerPendingAccount(any(), any(), any(), any()))
                .thenReturn(pendingAccount(accountId));
        doThrow(new IllegalStateException("mail failure"))
                .when(identityProviderAdminPortOut).sendVerifyEmail("keycloak-user-id");

        useCase.submitRegistration(validCommand());

        verify(partnerRegistrationPortOut).save(any());
        verify(identityProviderAdminPortOut, never()).deleteUser("keycloak-user-id");
    }

    @Test
    void shouldRejectDuplicateUsernameBeforeCreatingIdentity() {
        when(accountRegistrationPortOut.existsByUsername("partner.admin")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.submitRegistration(validCommand()));

        verify(identityProviderAdminPortOut, never()).createUser(any());
    }

    @Test
    void shouldRejectDuplicatePhoneNumberBeforeCreatingIdentity() {
        when(userProfilePortOut.existsByPhoneNumber("0901234567")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.submitRegistration(validCommand()));

        verify(identityProviderAdminPortOut, never()).createUser(any());
    }

    @Test
    void shouldStoreFullNameAndPhoneNumberInMinimalUserProfile() {
        UUID accountId = UUID.randomUUID();
        when(identityProviderAdminPortOut.createUser(any())).thenReturn("keycloak-user-id");
        when(accountRegistrationPortOut.registerPendingAccount(
                any(), eq("keycloak-user-id"), any(), eq(AdminProvisionableAccountRoleCode.PARTNER_ADMIN)
        )).thenReturn(pendingAccount(accountId));

        useCase.submitRegistration(validCommand());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(accountRegistrationPortOut).registerPendingAccount(
                any(), eq("keycloak-user-id"), profileCaptor.capture(), eq(AdminProvisionableAccountRoleCode.PARTNER_ADMIN));
        assertEquals("Nguyễn Văn A", profileCaptor.getValue().getFullName());
        assertEquals("0901234567", profileCaptor.getValue().getPhoneNumber());
    }

    @Test
    void shouldAllowNonSystemAdminReviewerWithOrganizationCreatePermission() {
        UUID reviewerId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(reviewerId);
        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));
        when(provisionedAccountPortOut.findProvisionedAccountById(applicantId))
                .thenReturn(Optional.of(pendingPartnerApplicant(applicantId, userProfileId)));
        when(identityProviderAdminPortOut.isEmailVerified("partner-keycloak-id")).thenReturn(true);
        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        when(organizationPortOut.save(any())).thenReturn(organization);

        // Mock user profile with all required fields
        UserProfile userProfile = completeUserProfile(userProfileId);
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Mock active avatar
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(userProfileId);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        when(userProfileAvatarPortIn.findAllByUserProfileId(userProfileId)).thenReturn(List.of(avatar));

        useCase.approveRegistration(approval.getApprovalRequestId(), new ReviewPartnerRegistrationCommand("Đủ điều kiện"));

        verify(currentAccountPortIn).requirePermission("ORGANIZATION_CREATE_ALL");
        verify(provisionedAccountPortOut).updateProvisionedAccountStatus(
                eq(applicantId), eq(AccountStatus.ACTIVE), any(), eq(null), eq(null), eq(reviewerId), any()
        );
        verify(organizationPortOut).createActiveMembership(organization.getOrganizationId(), applicantId);
        assertEquals(ApprovalRequestStatus.APPROVED, approval.getStatus());
        verify(notificationSender).send(argThat(command ->
                applicantId.equals(command.accountId())
                        && NotificationType.ACCOUNT_STATUS_CHANGED.equals(command.notificationType())
                        && approval.getApprovalRequestId().equals(command.relatedId())
        ));
    }

    @Test
    void shouldRejectApprovalWhenApplicantEmailIsNotVerified() {
        UUID applicantId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));
        when(provisionedAccountPortOut.findProvisionedAccountById(applicantId))
                .thenReturn(Optional.of(pendingPartnerApplicant(applicantId, userProfileId)));

        assertThrows(
                ConflictException.class,
                () -> useCase.approveRegistration(approval.getApprovalRequestId(), new ReviewPartnerRegistrationCommand(null))
        );

        verify(provisionedAccountPortOut, never()).updateProvisionedAccountStatus(any(), any(), any(), any(), any(), any(), any());
        verify(organizationPortOut, never()).save(any());
    }

    @Test
    void shouldAutoApproveEligibleVerifiedPartnerUsingSystemActor() {
        UUID applicantId = UUID.randomUUID();
        UUID systemAccountId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);
        OnboardingApprovalPolicy policy = enabledPartnerPolicy(approval.getCreatedAt().minusSeconds(1));
        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());

        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));
        when(onboardingApprovalPolicyEvaluator.findEligiblePolicy(
                OnboardingApprovalPolicyType.PARTNER_REGISTRATION,
                approval.getCreatedAt()
        )).thenReturn(Optional.of(policy));
        when(provisionedAccountPortOut.findProvisionedAccountById(applicantId))
                .thenReturn(Optional.of(pendingPartnerApplicant(applicantId, userProfileId)));
        when(identityProviderAdminPortOut.isEmailVerified("partner-keycloak-id")).thenReturn(true);
        when(systemAccountIdPortOut.getSystemAccountId()).thenReturn(systemAccountId);
        when(organizationPortOut.save(any())).thenReturn(organization);

        // Mock user profile with all required fields
        UserProfile userProfile = completeUserProfile(userProfileId);
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(userProfile));

        // Mock active avatar
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(userProfileId);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        when(userProfileAvatarPortIn.findAllByUserProfileId(userProfileId)).thenReturn(List.of(avatar));

        var result = useCase.tryAutoApproveRegistration(approval.getApprovalRequestId());

        assertTrue(result.isPresent());
        assertEquals(ApprovalRequestStatus.APPROVED, approval.getStatus());
        assertEquals("AUTO", approval.getDecisionData().get("decisionMode"));
        verify(provisionedAccountPortOut).updateProvisionedAccountStatus(
                eq(applicantId), eq(AccountStatus.ACTIVE), any(), eq(null), eq(null), eq(systemAccountId), any()
        );
        verify(organizationPortOut).createActiveMembership(organization.getOrganizationId(), applicantId);
        verify(partnerCatalogSeedPortOut).copyInternalCatalog(organization.getOrganizationId());
        verify(currentAccountPortIn, never()).getCurrentAccountIdOrThrow();
    }

    @Test
    void shouldRequireReviewNoteWhenRejecting() {
        UUID applicantId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);

        assertThrows(
                BadRequestException.class,
                () -> useCase.rejectRegistration(approval.getApprovalRequestId(), new ReviewPartnerRegistrationCommand(null))
        );
        assertThrows(
                BadRequestException.class,
                () -> useCase.rejectRegistration(approval.getApprovalRequestId(), new ReviewPartnerRegistrationCommand("   "))
        );

        assertEquals(ApprovalRequestStatus.PENDING, approval.getStatus());
        verify(partnerRegistrationPortOut, never()).save(any());
    }

    @Test
    void shouldReturnApplicantInfoFromProfileInReviewResult() {
        UUID reviewerId = UUID.randomUUID();
        UUID applicantId = UUID.randomUUID();
        UUID userProfileId = UUID.randomUUID();
        ApprovalRequest approval = pendingApproval(applicantId, userProfileId);
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(reviewerId);
        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));
        when(provisionedAccountPortOut.findProvisionedAccountById(applicantId))
                .thenReturn(Optional.of(pendingPartnerApplicant(applicantId, userProfileId)));
        when(identityProviderAdminPortOut.isEmailVerified("partner-keycloak-id")).thenReturn(true);
        Organization organization = new Organization();
        organization.setOrganizationId(UUID.randomUUID());
        when(organizationPortOut.save(any())).thenReturn(organization);
        UserProfile userProfile = completeUserProfile(userProfileId);
        when(userProfilePortOut.findById(userProfileId)).thenReturn(Optional.of(userProfile));
        UserProfileAvatar avatar = new UserProfileAvatar();
        avatar.setAvatarId(UUID.randomUUID());
        avatar.setUserProfileId(userProfileId);
        avatar.setStatus(UserProfileAvatarStatus.ACTIVE);
        avatar.setCurrent(true);
        when(userProfileAvatarPortIn.findAllByUserProfileId(userProfileId)).thenReturn(List.of(avatar));

        var result = useCase.approveRegistration(approval.getApprovalRequestId(), new ReviewPartnerRegistrationCommand("Đủ điều kiện"));

        assertEquals("Nguyễn Văn A", result.applicantFullName());
        assertEquals("0901234567", result.applicantPhoneNumber());
        assertEquals("partner@example.com", result.applicantEmail());
        assertEquals("PARTNER_ABC", result.organizationCode());
        assertEquals("Bãi xe ABC", result.organizationName());
    }

    @Test
    void shouldDenyReviewerWithoutOrganizationCreatePermissionRegardlessOfRole() {
        doThrow(new AccessDeniedException("missing permission"))
                .when(currentAccountPortIn).requirePermission("ORGANIZATION_CREATE_ALL");

        assertThrows(
                AccessDeniedException.class,
                () -> useCase.rejectRegistration(UUID.randomUUID(), new ReviewPartnerRegistrationCommand("No"))
        );

        verify(partnerRegistrationPortOut, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldRejectReviewWhenRequestWasAlreadyReviewed() {
        ApprovalRequest approval = pendingApproval(UUID.randomUUID());
        approval.setStatus(ApprovalRequestStatus.APPROVED);
        when(partnerRegistrationPortOut.findByIdForUpdate(approval.getApprovalRequestId()))
                .thenReturn(Optional.of(approval));

        assertThrows(
                ConflictException.class,
                () -> useCase.rejectRegistration(
                        approval.getApprovalRequestId(),
                        new ReviewPartnerRegistrationCommand("Already handled")
                )
        );

        verify(partnerRegistrationPortOut, never()).save(any());
    }

    private CreatePartnerRegistrationCommand validCommand() {
        return new CreatePartnerRegistrationCommand(
                " Nguyễn Văn A ",
                " partner.admin ",
                "StrongPassword1!",
                " partner_abc ",
                " Bãi xe ABC ",
                "PARTNER@example.com",
                "0901234567"
        );
    }

    private Account pendingAccount(UUID accountId) {
        Account account = new Account();
        account.setAccountId(accountId);
        account.setStatus(AccountStatus.PENDING);
        account.setEmail("partner@example.com");
        account.setKeycloakUserId("keycloak-user-id");
        return account;
    }

    private ProvisionedAccountResult pendingPartnerApplicant(UUID accountId) {
        return pendingPartnerApplicant(accountId, UUID.randomUUID());
    }

    private ProvisionedAccountResult pendingPartnerApplicant(UUID accountId, UUID userProfileId) {
        return new ProvisionedAccountResult(
                new ProvisionedAccountResult.AccountInfoResult(
                        accountId,
                        userProfileId,
                        "partner-keycloak-id",
                        "partner.admin",
                        "partner@example.com",
                        AccountStatus.PENDING,
                        Instant.now(),
                        Instant.now()
                ),
                new ProvisionedAccountResult.RoleInfoResult(
                        UUID.randomUUID(),
                        "PARTNER_ADMIN",
                        "Partner Admin",
                        Set.of("ORGANIZATION_READ_ALL").stream().toList()
                )
        );
    }

    private ApprovalRequest pendingApproval(UUID applicantId) {
        return pendingApproval(applicantId, UUID.randomUUID());
    }

    private ApprovalRequest pendingApproval(UUID applicantId, UUID userProfileId) {
        ApprovalRequest approval = new ApprovalRequest();
        approval.setApprovalRequestId(UUID.randomUUID());
        approval.setRequestType(PartnerRegistrationUseCaseImpl.REQUEST_TYPE);
        approval.setTargetSchema(PartnerRegistrationUseCaseImpl.TARGET_SCHEMA);
        approval.setTargetTable(PartnerRegistrationUseCaseImpl.TARGET_TABLE);
        approval.setTargetId(applicantId);
        approval.setRequestedBy(applicantId);
        approval.setStatus(ApprovalRequestStatus.PENDING);
        approval.setCreatedAt(Instant.now());
        approval.setRequestData(Map.of(
                "organizationCode", "PARTNER_ABC",
                "organizationName", "Bãi xe ABC",
                "organizationAddressDetail", "123 Đường ABC",
                "organizationWardCode", "ward-001",
                "organizationDistrictCode", "district-001",
                "organizationAddressDisplay", "123 Đường ABC, Phường ABC, Quận ABC, Thành phố ABC"
        ));
        return approval;
    }

    private UserProfile completeUserProfile(UUID userProfileId) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(userProfileId);
        userProfile.setFullName("Nguyễn Văn A");
        userProfile.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        userProfile.setGender("Nam");
        userProfile.setPhoneNumber("0901234567");
        userProfile.setIdentifyCard("123456789012");
        userProfile.setAddressDetail("123 Đường ABC");
        userProfile.setWardCode("ward-001");
        userProfile.setDistrictCode("district-001");
        userProfile.setAddressDisplay("123 Đường ABC, Phường ABC, Quận ABC, Thành phố ABC");
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        return userProfile;
    }

    private VietnamAddress legacyAddress(String addressDetail) {
        VietnamAddress address = VietnamAddress.ofLegacy(
                "79",
                "760",
                "27013",
                addressDetail
        );
        address.setAddressDisplay(addressDetail + ", Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        return address;
    }

    private OnboardingApprovalPolicy enabledPartnerPolicy(Instant effectiveFrom) {
        OnboardingApprovalPolicy policy = new OnboardingApprovalPolicy();
        policy.setPolicyId(UUID.randomUUID());
        policy.setPolicyType(OnboardingApprovalPolicyType.PARTNER_REGISTRATION);
        policy.setAutoApproveEnabled(true);
        policy.setEffectiveFrom(effectiveFrom);
        policy.setVersion(1L);
        return policy;
    }

    @SuppressWarnings("unused")
    private CurrentAccountAccess reviewer(UUID accountId, String roleCode) {
        return new CurrentAccountAccess(
                accountId,
                "subject",
                "reviewer",
                "reviewer@example.com",
                UUID.randomUUID(),
                roleCode,
                AccountStatus.ACTIVE,
                null,
                Set.of("ORGANIZATION_CREATE_ALL")
        );
    }
}
