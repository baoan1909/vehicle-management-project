package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.catalog.seed.port.out.PartnerCatalogSeedPortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CompletePartnerProfileCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.application.operations.approvalrequest.usecase.OnboardingApprovalPolicyEvaluator;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.usecase.RequiresNewNotificationSender;
import com.ban.vehicle_management.application.people.userprofile.port.in.UserProfileAvatarPortIn;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.organization.policy.OrganizationPolicy;
import com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerApprovalValidator;
import com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerRegistrationPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfileAvatar;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.domain.shared.address.VietnamAddress;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileAvatarStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerRegistrationUseCaseImpl implements PartnerRegistrationPortIn {

    public static final String REQUEST_TYPE = "PARTNER_REGISTRATION";
    public static final String TARGET_SCHEMA = "iam";
    public static final String TARGET_TABLE = "accounts";
    public static final String LEGACY_TARGET_TABLE = "organizations";
    private static final String ORGANIZATION_READ_ALL = "ORGANIZATION_READ_ALL";
    private static final String ORGANIZATION_CREATE_ALL = "ORGANIZATION_CREATE_ALL";
    private static final String NEXT_VERIFY_EMAIL = "VERIFY_EMAIL";
    private static final Logger LOGGER = LoggerFactory.getLogger(PartnerRegistrationUseCaseImpl.class);

    private final PartnerRegistrationPortOut partnerRegistrationPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final AccountRegistrationPortOut accountRegistrationPortOut;
    private final IdentityProviderAdminPortOut identityProviderAdminPortOut;
    private final ProvisionedAccountPortOut provisionedAccountPortOut;
    private final SystemAccountIdPortOut systemAccountIdPortOut;
    private final OrganizationPortOut organizationPortOut;
    private final PartnerCatalogSeedPortOut partnerCatalogSeedPortOut;
    private final OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator;
    private final PublicAuthPolicy publicAuthPolicy;
    private final VehicleMailService vehicleMailService;
    private final RequiresNewNotificationSender notificationSender;
    private final UserProfilePortOut userProfilePortOut;
    private final UserProfileAvatarPortIn userProfileAvatarPortIn;
    private final PartnerRegistrationPolicy partnerRegistrationPolicy = new PartnerRegistrationPolicy();
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final UserProfilePolicy userProfilePolicy;
    private final OrganizationPolicy organizationPolicy;
    private final PartnerApprovalValidator partnerApprovalValidator;

    public PartnerRegistrationUseCaseImpl(
            PartnerRegistrationPortOut partnerRegistrationPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            AccountRegistrationPortOut accountRegistrationPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            ProvisionedAccountPortOut provisionedAccountPortOut,
            SystemAccountIdPortOut systemAccountIdPortOut,
            OrganizationPortOut organizationPortOut,
            PartnerCatalogSeedPortOut partnerCatalogSeedPortOut,
            OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator,
            PublicAuthPolicy publicAuthPolicy,
            VehicleMailService vehicleMailService,
            RequiresNewNotificationSender notificationSender,
            UserProfilePortOut userProfilePortOut,
            UserProfileAvatarPortIn userProfileAvatarPortIn,
            UserProfilePolicy userProfilePolicy,
            OrganizationPolicy organizationPolicy,
            PartnerApprovalValidator partnerApprovalValidator
    ) {
        this.partnerRegistrationPortOut = partnerRegistrationPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountRegistrationPortOut = accountRegistrationPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.provisionedAccountPortOut = provisionedAccountPortOut;
        this.systemAccountIdPortOut = systemAccountIdPortOut;
        this.organizationPortOut = organizationPortOut;
        this.partnerCatalogSeedPortOut = partnerCatalogSeedPortOut;
        this.onboardingApprovalPolicyEvaluator = onboardingApprovalPolicyEvaluator;
        this.publicAuthPolicy = publicAuthPolicy;
        this.vehicleMailService = vehicleMailService;
        this.notificationSender = notificationSender;
        this.userProfilePortOut = userProfilePortOut;
        this.userProfileAvatarPortIn = userProfileAvatarPortIn;
        this.userProfilePolicy = userProfilePolicy;
        this.organizationPolicy = organizationPolicy;
        this.partnerApprovalValidator = partnerApprovalValidator;
    }

    @Override
    @Transactional
    public PartnerRegistrationSubmissionResult submitRegistration(CreatePartnerRegistrationCommand command) {
        CreatePartnerRegistrationCommand normalizedPartner = partnerRegistrationPolicy.normalize(command);
        RegisterAccountCommand normalizedAccount = publicAuthPolicy.normalizeRegisterCommand(
                new RegisterAccountCommand(
                        normalizedPartner.username(),
                        normalizedPartner.email(),
                        normalizedPartner.password(),
                        normalizedPartner.fullName()
                )
        );
        ensureRegistrationHasNoConflict(normalizedPartner, normalizedAccount);

        String keycloakUserId = identityProviderAdminPortOut.createUser(normalizedAccount);
        AtomicBoolean keycloakUserDeleted = new AtomicBoolean(false);
        TransactionalEvents.runAfterRollback(() -> deleteKeycloakUserSafely(keycloakUserId, keycloakUserDeleted));
        try {
            Account account = accountRegistrationPortOut.registerPendingAccount(
                    normalizedAccount,
                    keycloakUserId,
                    buildMinimalUserProfile(normalizedAccount.fullName(), normalizedPartner.phoneNumber()),
                    AdminProvisionableAccountRoleCode.PARTNER_ADMIN
            );
            identityProviderAdminPortOut.updateAccountIdAttribute(keycloakUserId, account.getAccountId());

            ApprovalRequest approval = buildPendingApprovalRequest(account.getAccountId(), normalizedPartner);
            partnerRegistrationPortOut.save(approval);
            TransactionalEvents.runAfterCommit(() -> {
                sendVerificationEmailSafely(keycloakUserId, account.getAccountId());
                notifyApplicantSafely(
                        account.getAccountId(),
                        NotificationType.ACCOUNT_REGISTERED,
                        "Đăng ký đối tác thành công",
                        "Tài khoản đã được tạo. Vui lòng xác thực email và chờ hồ sơ được xét duyệt.",
                        approval.getApprovalRequestId()
                );
            });
            return new PartnerRegistrationSubmissionResult(
                    account.getAccountId(),
                    approval.getApprovalRequestId(),
                    AccountStatus.PENDING,
                    ApprovalRequestStatus.PENDING,
                    NEXT_VERIFY_EMAIL
            );
        } catch (RuntimeException exception) {
            deleteKeycloakUserSafely(keycloakUserId, keycloakUserDeleted);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerRegistrationResult> getPartnerRegistrations(ApprovalRequestStatus status) {
        currentAccountPortIn.requirePermission(ORGANIZATION_READ_ALL);
        return partnerRegistrationPortOut.findAll(status);
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerRegistrationResult getPartnerRegistration(UUID approvalRequestId) {
        currentAccountPortIn.requirePermission(ORGANIZATION_READ_ALL);
        if (approvalRequestId == null) {
            throw new NotFoundException("Partner registration not found");
        }
        return partnerRegistrationPortOut.findById(approvalRequestId)
                .map(this::toResult)
                .orElseThrow(() -> new NotFoundException("Partner registration not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAutoApprovalCandidateIds(int limit) {
        return onboardingApprovalPolicyEvaluator
                .findEffectiveFrom(OnboardingApprovalPolicyType.PARTNER_REGISTRATION)
                .map(effectiveFrom -> partnerRegistrationPortOut.findPendingIdsCreatedAtOrAfter(
                        effectiveFrom,
                        Math.min(Math.max(limit, 1), 200)
                ))
                .orElseGet(List::of);
    }

    @Override
    @Transactional
    public Optional<PartnerRegistrationResult> tryAutoApproveRegistration(UUID approvalRequestId) {
        if (approvalRequestId == null) {
            return Optional.empty();
        }
        ApprovalRequest approvalRequest = partnerRegistrationPortOut.findByIdForUpdate(approvalRequestId)
                .orElse(null);
        if (approvalRequest == null || approvalRequest.getStatus() != ApprovalRequestStatus.PENDING) {
            return Optional.empty();
        }
        Optional<OnboardingApprovalPolicy> eligiblePolicy = onboardingApprovalPolicyEvaluator.findEligiblePolicy(
                OnboardingApprovalPolicyType.PARTNER_REGISTRATION,
                approvalRequest.getCreatedAt()
        );
        if (eligiblePolicy.isEmpty()) {
            return Optional.empty();
        }

        ProvisionedAccountResult applicant = getApplicantAccount(requireApplicantAccountId(approvalRequest));
        ensurePendingPartnerAdmin(applicant);
        if (!identityProviderAdminPortOut.isEmailVerified(applicant.account().keycloakUserId())) {
            return Optional.empty();
        }

        // Validate full profile before auto-approval
        try {
            validateProfileForApproval(approvalRequest, applicant);
        } catch (RuntimeException e) {
            LOGGER.info("Auto-approval skipped for {}: {}", approvalRequestId, e.getMessage());
            return Optional.empty();
        }

        return Optional.of(approveRegistrationInternal(
                approvalRequest,
                applicant,
                systemAccountIdPortOut.getSystemAccountId(),
                "Automatically approved by onboarding policy",
                autoDecisionData(eligiblePolicy.get())
        ));
    }

    @Override
    @Transactional
    public PartnerRegistrationResult approveRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        UUID reviewerId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        ApprovalRequest approvalRequest = getPendingRegistrationForUpdate(approvalRequestId);
        ProvisionedAccountResult applicant = getApplicantAccount(requireApplicantAccountId(approvalRequest));
        ensurePendingPartnerAdmin(applicant);
        if (!identityProviderAdminPortOut.isEmailVerified(applicant.account().keycloakUserId())) {
            throw new ConflictException("Partner email must be verified before approval");
        }
        return approveRegistrationInternal(
                approvalRequest,
                applicant,
                reviewerId,
                normalizeReviewNote(command),
                Map.of("decisionMode", "MANUAL")
        );
    }

    @Override
    @Transactional
    public PartnerRegistrationResult rejectRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        String reviewNote = normalizeReviewNote(command);
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new BadRequestException("Review note is required when rejecting a partner registration");
        }
        ApprovalRequest approvalRequest = getPendingRegistrationForUpdate(approvalRequestId);
        ProvisionedAccountResult applicant = getApplicantAccount(requireApplicantAccountId(approvalRequest));
        ensurePendingPartnerAdmin(applicant);
        approvalRequestPolicy.reject(approvalRequest, reviewNote);
        partnerRegistrationPortOut.save(approvalRequest);
        scheduleReviewOutcome(approvalRequest, applicant, false);
        return toResult(approvalRequest);
    }

    private void ensureRegistrationHasNoConflict(
            CreatePartnerRegistrationCommand partner,
            RegisterAccountCommand account
    ) {
        if (accountRegistrationPortOut.existsByUsername(account.username())) {
            throw new ConflictException("Tên đăng nhập đã tồn tại.");
        }
        if (accountRegistrationPortOut.existsByEmail(account.email())) {
            throw new ConflictException("Email đã tồn tại.");
        }
        if (userProfilePortOut.existsByPhoneNumber(partner.phoneNumber())) {
            throw new ConflictException("Số điện thoại đã được sử dụng.");
        }
        if (partnerRegistrationPortOut.existsPendingByOrganizationCode(partner.organizationCode())) {
            throw new ConflictException("A partner registration with this organization code is already pending");
        }
        if (organizationPortOut.existsByCode(partner.organizationCode())) {
            throw new ConflictException("Organization code already exists");
        }
    }

    private ApprovalRequest buildPendingApprovalRequest(
            UUID accountId,
            CreatePartnerRegistrationCommand partner
    ) {
        ApprovalRequest approval = new ApprovalRequest();
        approval.setApprovalRequestId(UUID.randomUUID());
        approval.setRequestType(REQUEST_TYPE);
        approval.setTargetSchema(TARGET_SCHEMA);
        approval.setTargetTable(TARGET_TABLE);
        approval.setTargetId(accountId);
        approval.setRequestedBy(accountId);
        approval.setStatus(ApprovalRequestStatus.PENDING);
        approval.setRequestData(Map.ofEntries(
                Map.entry("organizationCode", partner.organizationCode()),
                Map.entry("organizationName", partner.organizationName())
        ));
        approvalRequestPolicy.initialize(approval);
        return approval;
    }

    private UserProfile buildMinimalUserProfile(String fullName, String phoneNumber) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(UUID.randomUUID());
        userProfile.setFullName(fullName);
        userProfile.setPhoneNumber(phoneNumber);
        userProfile.setStatus(UserProfileStatus.ACTIVE);
        userProfilePolicy.initialize(userProfile);
        return userProfile;
    }

    private ApprovalRequest getPendingRegistrationForUpdate(UUID approvalRequestId) {
        if (approvalRequestId == null) {
            throw new NotFoundException("Partner registration not found");
        }
        ApprovalRequest approvalRequest = partnerRegistrationPortOut.findByIdForUpdate(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Partner registration not found"));
        if (approvalRequest.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Partner registration has already been reviewed");
        }
        return approvalRequest;
    }

    private UUID requireApplicantAccountId(ApprovalRequest approvalRequest) {
        UUID applicantAccountId = approvalRequest.getRequestedBy();
        if (applicantAccountId == null) {
            throw new ConflictException("Legacy partner registration is not linked to an applicant account");
        }
        return applicantAccountId;
    }

    private ProvisionedAccountResult getApplicantAccount(UUID accountId) {
        return provisionedAccountPortOut.findProvisionedAccountById(accountId)
                .orElseThrow(() -> new NotFoundException("Partner applicant account not found"));
    }

    private UserProfile getApplicantUserProfile(UUID accountId) {
        ProvisionedAccountResult applicant = getApplicantAccount(accountId);
        return userProfilePortOut.findById(applicant.account().userProfileId())
                .orElseThrow(() -> new NotFoundException("Partner applicant user profile not found"));
    }

    private void ensurePendingPartnerAdmin(ProvisionedAccountResult applicant) {
        if (!AdminProvisionableAccountRoleCode.PARTNER_ADMIN.name().equals(applicant.role().roleCode())) {
            throw new ConflictException("Partner applicant account must have PARTNER_ADMIN role");
        }
        if (!AccountStatus.PENDING.equals(applicant.account().accountStatus())) {
            throw new ConflictException("Partner applicant account is not pending");
        }
    }

    private PartnerRegistrationResult approveRegistrationInternal(
            ApprovalRequest approvalRequest,
            ProvisionedAccountResult applicant,
            UUID actorAccountId,
            String note,
            Map<String, String> decisionMetadata
    ) {
        // Validate full profile before approval
        validateProfileForApproval(approvalRequest, applicant);

        String organizationCode = approvalRequest.getRequestData().get("organizationCode");
        if (organizationPortOut.existsByCode(organizationCode)) {
            throw new ConflictException("Organization code already exists");
        }

        provisionedAccountPortOut.updateProvisionedAccountStatus(
                applicant.account().accountId(),
                AccountStatus.ACTIVE,
                UserProfileStatus.ACTIVE,
                null,
                null,
                actorAccountId,
                "Partner registration approved"
        );

        // Build organization with structured address
        Organization organization = new Organization();
        organization.setCode(organizationCode);
        organization.setName(approvalRequest.getRequestData().get("organizationName"));
        // Set structured address from requestData
        VietnamAddress orgAddress = toOrganizationAddress(approvalRequest.getRequestData());
        if (orgAddress != null) {
            organization.setStructuredAddress(orgAddress);
        }
        organization.setStatus(OrganizationStatus.ACTIVE);
        organizationPolicy.initialize(organization);
        organization.setOrganizationId(UUID.randomUUID());
        Organization createdOrganization = organizationPortOut.save(organization);
        organizationPortOut.createActiveMembership(
                createdOrganization.getOrganizationId(),
                applicant.account().accountId()
        );
        partnerCatalogSeedPortOut.copyInternalCatalog(createdOrganization.getOrganizationId());

        Map<String, String> decisionData = new HashMap<>(decisionMetadata);
        decisionData.put("organizationId", createdOrganization.getOrganizationId().toString());
        approvalRequest.setDecisionData(Map.copyOf(decisionData));
        approvalRequestPolicy.approve(approvalRequest, actorAccountId, Instant.now(), note);
        partnerRegistrationPortOut.save(approvalRequest);
        scheduleReviewOutcome(approvalRequest, applicant, true);
        return toResult(approvalRequest);
    }

    private void validateProfileForApproval(ApprovalRequest approvalRequest, ProvisionedAccountResult applicant) {
        UserProfile userProfile = getApplicantUserProfile(applicant.account().accountId());
        List<UserProfileAvatar> avatars = userProfileAvatarPortIn.findAllByUserProfileId(userProfile.getUserProfileId());
        userProfilePolicy.validateState(userProfile);
        if (userProfile.getPhoneNumber() != null
                && userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(
                        userProfile.getPhoneNumber(),
                        userProfile.getUserProfileId()
                )) {
            throw new ConflictException("Phone number already exists");
        }
        if (userProfile.getIdentifyCard() != null
                && userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(
                        userProfile.getIdentifyCard(),
                        userProfile.getUserProfileId()
                )) {
            throw new ConflictException("Identify card already exists");
        }

        // Get organization data from requestData
        String organizationCode = approvalRequest.getRequestData().get("organizationCode");
        String organizationName = approvalRequest.getRequestData().get("organizationName");

        // Build organization address from requestData
        VietnamAddress structuredOrganizationAddress = toOrganizationAddress(approvalRequest.getRequestData());
        Organization.OrganizationAddress orgAddress = null;
        if (structuredOrganizationAddress != null) {
            Organization organizationDraft = new Organization();
            organizationDraft.setCode(organizationCode);
            organizationDraft.setName(organizationName);
            organizationDraft.setStructuredAddress(structuredOrganizationAddress);
            organizationPolicy.initialize(organizationDraft);
            orgAddress = new Organization.OrganizationAddress();
            orgAddress.setStructuredAddress(organizationDraft.getStructuredAddress());
        }

        // Create Account domain model from AccountInfoResult
        Account account = new Account();
        account.setAccountId(applicant.account().accountId());
        account.setUserProfileId(applicant.account().userProfileId());
        account.setKeycloakUserId(applicant.account().keycloakUserId());
        account.setUsername(applicant.account().username());
        account.setEmail(applicant.account().email());
        account.setRoleId(applicant.role().roleId());
        account.setStatus(applicant.account().accountStatus());
        account.setFailedLoginCount(0);
        account.setCreatedAt(applicant.account().createdAt());
        account.setUpdatedAt(applicant.account().updatedAt());

        partnerApprovalValidator.validateBeforeApproval(
                approvalRequest,
                account,
                userProfile,
                avatars,
                organizationCode,
                organizationName,
                orgAddress
        );
    }

    private Map<String, String> autoDecisionData(OnboardingApprovalPolicy policy) {
        Map<String, String> data = new HashMap<>();
        data.put("decisionMode", "AUTO");
        data.put("policyType", policy.getPolicyType().name());
        data.put("policyVersion", String.valueOf(policy.getVersion()));
        data.put("effectiveFrom", policy.getEffectiveFrom().toString());
        if (policy.getUpdatedBy() != null) {
            data.put("configuredBy", policy.getUpdatedBy().toString());
        }
        return Map.copyOf(data);
    }

    private String normalizeReviewNote(ReviewPartnerRegistrationCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    @Override
    @Transactional
    public void completeMyProfile(CompletePartnerProfileCommand command) {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        ApprovalRequest approval = partnerRegistrationPortOut.findLatestByApplicantAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Partner registration not found"));
        if (!accountId.equals(approval.getRequestedBy())) {
            throw new NotFoundException("Partner registration not found");
        }
        if (approval.getStatus() != ApprovalRequestStatus.PENDING) {
            throw new ConflictException("Profile can only be updated while approval is pending");
        }

        ProvisionedAccountResult applicant = getApplicantAccount(accountId);
        ensurePendingPartnerAdmin(applicant);
        if (!identityProviderAdminPortOut.isEmailVerified(applicant.account().keycloakUserId())) {
            throw new ConflictException("Partner email must be verified before completing the profile");
        }

        // Validate and update personal profile
        UserProfile userProfile = getApplicantUserProfile(accountId);
        if (userProfile == null) {
            throw new NotFoundException("User profile not found");
        }

        CompletePartnerProfileCommand normalized = normalizeCompleteCommand(command);
        if (normalized.personalAddress() == null || normalized.organizationAddress() == null) {
            throw new BadRequestException("Personal and organization addresses are required");
        }
        String registeredOrganizationCode = approval.getRequestData().get("organizationCode");
        if (!normalized.organizationCode().equals(registeredOrganizationCode)) {
            throw new ConflictException("Organization code cannot be changed after registration");
        }

        // Check uniqueness for phone number and identify card
        if (normalized.phoneNumber() != null
                && userProfilePortOut.existsByPhoneNumberAndUserProfileIdNot(normalized.phoneNumber(), userProfile.getUserProfileId())) {
            throw new ConflictException("Phone number already exists");
        }
        if (normalized.identifyCard() != null
                && userProfilePortOut.existsByIdentifyCardAndUserProfileIdNot(normalized.identifyCard(), userProfile.getUserProfileId())) {
            throw new ConflictException("Identify card already exists");
        }

        // Update personal profile fields
        userProfile.setFullName(normalized.fullName());
        userProfile.setDateOfBirth(normalized.dateOfBirth());
        userProfile.setGender(normalized.gender());
        userProfile.setPhoneNumber(normalized.phoneNumber());
        userProfile.setIdentifyCard(normalized.identifyCard());
        if (normalized.personalAddress() != null) {
            userProfile.setStructuredAddress(normalized.personalAddress());
        }
        userProfilePolicy.validateState(userProfile);
        userProfilePortOut.save(userProfile);

        Organization organizationDraft = new Organization();
        organizationDraft.setCode(normalized.organizationCode());
        organizationDraft.setName(normalized.organizationName());
        organizationDraft.setStructuredAddress(normalized.organizationAddress());
        organizationPolicy.initialize(organizationDraft);

        // Update organization data in approval request
        Map<String, String> requestData = new HashMap<>(approval.getRequestData());
        if (normalized.organizationCode() != null) {
            requestData.put("organizationCode", normalized.organizationCode());
        }
        if (normalized.organizationName() != null) {
            requestData.put("organizationName", normalized.organizationName());
        }
        if (normalized.organizationAddress() != null) {
            VietnamAddress orgAddr = organizationDraft.getStructuredAddress();
            requestData.put("organizationAddressDetail", orgAddr.getAddressDetail());
            requestData.put("organizationProvinceCode", orgAddr.getProvinceCode());
            requestData.put("organizationWardCode", orgAddr.getWardCode());
            putOrRemove(requestData, "organizationDistrictCode", orgAddr.getDistrictCode());
            requestData.put("organizationAddressDisplay", orgAddr.getAddressDisplay());
        }
        approval.setRequestData(Map.copyOf(requestData));
        partnerRegistrationPortOut.save(approval);

        // Try auto-approval if profile is complete
        if (approval.getStatus() == ApprovalRequestStatus.PENDING) {
            tryAutoApproveRegistration(approval.getApprovalRequestId());
        }
        if (approval.getStatus() == ApprovalRequestStatus.PENDING) {
            schedulePartnerSubmissionNotifications(approval, accountId);
        }
    }

    private CompletePartnerProfileCommand normalizeCompleteCommand(CompletePartnerProfileCommand command) {
        if (command == null) {
            throw new BadRequestException("Partner profile request must not be null");
        }
        return new CompletePartnerProfileCommand(
                TextValidationUtils.normalizeRequiredText(command.fullName(), "fullName", 150),
                command.dateOfBirth(),
                TextValidationUtils.normalizeNullableText(command.gender(), "gender", 20),
                TextValidationUtils.normalizePhoneNumber(command.phoneNumber(), "phoneNumber", 20),
                TextValidationUtils.normalizeAlphaNumeric(command.identifyCard(), "identifyCard", 50),
                command.personalAddress(),
                TextValidationUtils.normalizeCode(command.organizationCode(), "organizationCode", 50),
                TextValidationUtils.normalizeRequiredText(command.organizationName(), "organizationName", 150),
                command.organizationAddress()
        );
    }

    private VietnamAddress toOrganizationAddress(Map<String, String> requestData) {
        String addressDetail = requestData.get("organizationAddressDetail");
        String provinceCode = requestData.get("organizationProvinceCode");
        String wardCode = requestData.get("organizationWardCode");
        if (addressDetail == null || provinceCode == null || wardCode == null) {
            return null;
        }
        VietnamAddress address = new VietnamAddress();
        address.setAddressDetail(addressDetail);
        address.setProvinceCode(provinceCode);
        address.setWardCode(wardCode);
        address.setDistrictCode(requestData.get("organizationDistrictCode"));
        address.setAddressDisplay(requestData.get("organizationAddressDisplay"));
        return address;
    }

    private void putOrRemove(Map<String, String> data, String key, String value) {
        if (value == null || value.isBlank()) {
            data.remove(key);
            return;
        }
        data.put(key, value);
    }

    private void schedulePartnerSubmissionNotifications(ApprovalRequest approvalRequest, UUID applicantAccountId) {
        UUID approvalRequestId = approvalRequest.getApprovalRequestId();
        TransactionalEvents.runAfterCommit(() -> {
            notifyApplicantSafely(
                    applicantAccountId,
                    NotificationType.ACCOUNT_PROFILE_SUBMITTED,
                    "Hồ sơ đối tác đã được gửi duyệt",
                    "Hồ sơ cá nhân và thông tin đơn vị của bạn đang chờ xét duyệt.",
                    approvalRequestId
            );
            notifyPartnerReviewers(
                    approvalRequestId,
                    approvalRequest.getTargetSchema(),
                    approvalRequest.getTargetTable(),
                    applicantAccountId,
                    "Có hồ sơ đối tác cần duyệt"
            );
        });
    }

    private void notifyPartnerReviewers(
            UUID approvalRequestId,
            String targetSchema,
            String targetTable,
            UUID applicantAccountId,
            String title
    ) {
        if (notificationSender == null) {
            return;
        }
        try {
            notificationSender.sendBroadcast(new BroadcastNotificationCommand(
                    true,
                    null,
                    null,
                    null,
                    NotificationType.ACCOUNT_PROFILE_SUBMITTED,
                    title,
                    "Có yêu cầu phê duyệt mới cần xử lý.",
                    "/admin/partner-registrations",
                    targetSchema,
                    targetTable,
                    approvalRequestId,
                    new NotificationRecipientCriteria(
                            true,
                            Set.of(ORGANIZATION_CREATE_ALL),
                            applicantAccountId == null ? Set.of() : Set.of(applicantAccountId),
                            true
                    )
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not send partner reviewer notification for approval {}", approvalRequestId, exception);
        }
    }

    private void scheduleReviewOutcome(
            ApprovalRequest approvalRequest,
            ProvisionedAccountResult applicant,
            boolean approved
    ) {
        String fullName = applicantFullName(applicant);
        String email = applicant.account().email();
        UUID approvalRequestId = approvalRequest.getApprovalRequestId();
        String targetSchema = approvalRequest.getTargetSchema();
        String targetTable = approvalRequest.getTargetTable();
        UUID applicantAccountId = applicant.account().accountId();
        TransactionalEvents.runAfterCommit(() -> {
            sendReviewEmailSafely(email, fullName, approvalRequest.getNote(), approved);
            notifyPartnerReviewers(
                    approvalRequestId,
                    targetSchema,
                    targetTable,
                    applicantAccountId,
                    approved ? "Đã duyệt một hồ sơ đối tác" : "Đã từ chối một hồ sơ đối tác"
            );
            if (approved) {
                notifyApplicantSafely(
                        applicant.account().accountId(),
                        NotificationType.ACCOUNT_STATUS_CHANGED,
                        "Hồ sơ đối tác đã được duyệt",
                        "Tài khoản đối tác của bạn đã được kích hoạt.",
                        approvalRequest.getApprovalRequestId()
                );
                return;
            }
            notifyApplicantSafely(
                    applicant.account().accountId(),
                    NotificationType.SYSTEM_NOTICE,
                    "Hồ sơ đối tác chưa được duyệt",
                    "Vui lòng xem lý do phản hồi trong trạng thái hồ sơ.",
                    approvalRequest.getApprovalRequestId()
            );
        });
    }

    private String applicantFullName(ProvisionedAccountResult applicant) {
        try {
            UserProfile userProfile = getApplicantUserProfile(applicant.account().accountId());
            if (userProfile.getFullName() != null && !userProfile.getFullName().isBlank()) {
                return userProfile.getFullName();
            }
        } catch (RuntimeException lookupFailure) {
            LOGGER.warn("Could not resolve applicant full name for account {}", applicant.account().accountId(), lookupFailure);
        }
        return applicant.account().username();
    }

    private void sendReviewEmailSafely(String email, String fullName, String note, boolean approved) {
        try {
            if (approved) {
                vehicleMailService.sendOnboardingApprovedEmail(email, fullName, "đối tác");
                return;
            }
            vehicleMailService.sendOnboardingRejectedEmail(email, fullName, "đối tác", note);
        } catch (RuntimeException exception) {
            LOGGER.warn("Partner review completed but result email could not be sent to {}", email, exception);
        }
    }

    private void sendVerificationEmailSafely(String keycloakUserId, UUID accountId) {
        try {
            identityProviderAdminPortOut.sendVerifyEmail(keycloakUserId);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Partner account {} created but verification email could not be sent. The user can request resend later.",
                    accountId,
                    exception
            );
        }
    }

    private void notifyApplicantSafely(
            UUID accountId,
            NotificationType type,
            String title,
            String message,
            UUID approvalRequestId
    ) {
        if (notificationSender == null) {
            return;
        }
        try {
            notificationSender.send(new SendNotificationCommand(
                    accountId,
                    type,
                    title,
                    message,
                    "/admin/profile",
                    "operations",
                    "approval_requests",
                    approvalRequestId
            ));
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not send partner registration notification for account {}", accountId, exception);
        }
    }

    private void deleteKeycloakUserSafely(String keycloakUserId, AtomicBoolean deleted) {
        if (!deleted.compareAndSet(false, true)) {
            return;
        }
        try {
            identityProviderAdminPortOut.deleteUser(keycloakUserId);
        } catch (RuntimeException cleanupException) {
            LOGGER.error("Could not compensate Keycloak user {} after partner registration failure", keycloakUserId, cleanupException);
        }
    }

    private PartnerRegistrationResult toResult(ApprovalRequest approvalRequest) {
        Map<String, String> data = approvalRequest.getRequestData();
        String applicantFullName = "";
        String applicantPhoneNumber = "";
        String applicantEmail = "";
        UUID applicantAccountId = approvalRequest.getRequestedBy();
        if (applicantAccountId != null) {
            try {
                ProvisionedAccountResult applicant = getApplicantAccount(applicantAccountId);
                applicantEmail = defaultString(applicant.account().email());
                UserProfile userProfile = getApplicantUserProfile(applicantAccountId);
                applicantFullName = defaultString(userProfile.getFullName());
                applicantPhoneNumber = defaultString(userProfile.getPhoneNumber());
            } catch (RuntimeException lookupFailure) {
                LOGGER.warn("Could not resolve applicant info for approval {}", approvalRequest.getApprovalRequestId(), lookupFailure);
            }
        }
        return new PartnerRegistrationResult(
                approvalRequest.getApprovalRequestId(),
                data.get("organizationCode"),
                data.get("organizationName"),
                data.get("organizationAddressDisplay"),
                applicantFullName,
                applicantPhoneNumber,
                applicantEmail,
                approvalRequest.getCreatedAt(),
                approvalRequest.getStatus(),
                approvalRequest.getNote()
        );
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
