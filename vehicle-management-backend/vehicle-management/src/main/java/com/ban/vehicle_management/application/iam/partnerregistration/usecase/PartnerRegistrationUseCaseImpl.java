package com.ban.vehicle_management.application.iam.partnerregistration.usecase;

import com.ban.vehicle_management.application.iam.account.model.command.RegisterAccountCommand;
import com.ban.vehicle_management.application.iam.account.model.result.ProvisionedAccountResult;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.account.port.out.AccountRegistrationPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.ProvisionedAccountPortOut;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.CreatePartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.command.ReviewPartnerRegistrationCommand;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerApplicationStatusResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationResult;
import com.ban.vehicle_management.application.iam.partnerregistration.model.result.PartnerRegistrationSubmissionResult;
import com.ban.vehicle_management.application.iam.partnerregistration.port.in.PartnerRegistrationPortIn;
import com.ban.vehicle_management.application.iam.partnerregistration.port.out.PartnerRegistrationPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.Account;
import com.ban.vehicle_management.domain.iam.account.policy.PublicAuthPolicy;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.domain.iam.partnerregistration.policy.PartnerRegistrationPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.userprofile.model.UserProfile;
import com.ban.vehicle_management.domain.people.userprofile.policy.UserProfilePolicy;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AdminProvisionableAccountRoleCode;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.people.UserProfileStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final OrganizationPortIn organizationPortIn;
    private final OrganizationPortOut organizationPortOut;
    private final PublicAuthPolicy publicAuthPolicy;
    private final VehicleMailService vehicleMailService;
    private final NotificationPortIn notificationPortIn;
    private final PartnerRegistrationPolicy partnerRegistrationPolicy = new PartnerRegistrationPolicy();
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final UserProfilePolicy userProfilePolicy = new UserProfilePolicy();

    public PartnerRegistrationUseCaseImpl(
            PartnerRegistrationPortOut partnerRegistrationPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            AccountRegistrationPortOut accountRegistrationPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            ProvisionedAccountPortOut provisionedAccountPortOut,
            OrganizationPortIn organizationPortIn,
            OrganizationPortOut organizationPortOut,
            PublicAuthPolicy publicAuthPolicy,
            VehicleMailService vehicleMailService,
            NotificationPortIn notificationPortIn
    ) {
        this.partnerRegistrationPortOut = partnerRegistrationPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.accountRegistrationPortOut = accountRegistrationPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.provisionedAccountPortOut = provisionedAccountPortOut;
        this.organizationPortIn = organizationPortIn;
        this.organizationPortOut = organizationPortOut;
        this.publicAuthPolicy = publicAuthPolicy;
        this.vehicleMailService = vehicleMailService;
        this.notificationPortIn = notificationPortIn;
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
                    buildMinimalUserProfile(normalizedAccount.fullName()),
                    AdminProvisionableAccountRoleCode.PARTNER_ADMIN
            );
            identityProviderAdminPortOut.updateAccountIdAttribute(keycloakUserId, account.getAccountId());

            ApprovalRequest approval = buildPendingApprovalRequest(account.getAccountId(), normalizedPartner, normalizedAccount);
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
    public PartnerApplicationStatusResult getMyRegistrationStatus() {
        UUID accountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        ApprovalRequest approval = partnerRegistrationPortOut.findLatestByApplicantAccountId(accountId)
                .orElseThrow(() -> new NotFoundException("Partner registration not found"));
        if (!accountId.equals(approval.getRequestedBy())) {
            throw new NotFoundException("Partner registration not found");
        }
        ProvisionedAccountResult applicant = getApplicantAccount(accountId);
        boolean emailVerified = identityProviderAdminPortOut.isEmailVerified(applicant.account().keycloakUserId());
        Map<String, String> data = approval.getRequestData();
        return new PartnerApplicationStatusResult(
                accountId,
                approval.getApprovalRequestId(),
                applicant.account().accountStatus(),
                approval.getStatus(),
                emailVerified,
                resolveNextAction(emailVerified, approval.getStatus(), applicant.account().accountStatus()),
                data.get("organizationCode"),
                data.get("organizationName"),
                approval.getNote(),
                approval.getCreatedAt()
        );
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
                reviewerId,
                "Partner registration approved"
        );

        Organization organization = new Organization();
        organization.setCode(organizationCode);
        organization.setName(approvalRequest.getRequestData().get("organizationName"));
        organization.setAddress(approvalRequest.getRequestData().get("address"));
        organization.setStatus(OrganizationStatus.ACTIVE);
        Organization createdOrganization = organizationPortIn.createOrganization(
                organization,
                applicant.account().accountId()
        );

        approvalRequest.setDecisionData(Map.of("organizationId", createdOrganization.getOrganizationId().toString()));
        approvalRequestPolicy.approve(
                approvalRequest,
                reviewerId,
                Instant.now(),
                normalizeReviewNote(command)
        );
        partnerRegistrationPortOut.save(approvalRequest);
        scheduleReviewOutcome(approvalRequest, applicant, true);
        return toResult(approvalRequest);
    }

    @Override
    @Transactional
    public PartnerRegistrationResult rejectRegistration(UUID approvalRequestId, ReviewPartnerRegistrationCommand command) {
        currentAccountPortIn.requirePermission(ORGANIZATION_CREATE_ALL);
        ApprovalRequest approvalRequest = getPendingRegistrationForUpdate(approvalRequestId);
        ProvisionedAccountResult applicant = getApplicantAccount(requireApplicantAccountId(approvalRequest));
        ensurePendingPartnerAdmin(applicant);
        approvalRequestPolicy.reject(approvalRequest, normalizeReviewNote(command));
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
        if (partnerRegistrationPortOut.existsPendingByEmail(account.email())) {
            throw new ConflictException("A partner registration with this email is already pending");
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
            CreatePartnerRegistrationCommand partner,
            RegisterAccountCommand account
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
                Map.entry("fullName", account.fullName()),
                Map.entry("username", account.username()),
                Map.entry("organizationCode", partner.organizationCode()),
                Map.entry("organizationName", partner.organizationName()),
                Map.entry("representativeName", partner.representativeName()),
                Map.entry("email", account.email()),
                Map.entry("phoneNumber", partner.phoneNumber())
        ));
        approvalRequestPolicy.initialize(approval);
        return approval;
    }

    private UserProfile buildMinimalUserProfile(String fullName) {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserProfileId(UUID.randomUUID());
        userProfile.setFullName(fullName);
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

    private void ensurePendingPartnerAdmin(ProvisionedAccountResult applicant) {
        if (!AdminProvisionableAccountRoleCode.PARTNER_ADMIN.name().equals(applicant.role().roleCode())) {
            throw new ConflictException("Partner applicant account must have PARTNER_ADMIN role");
        }
        if (!AccountStatus.PENDING.equals(applicant.account().accountStatus())) {
            throw new ConflictException("Partner applicant account is not pending");
        }
    }

    private String normalizeReviewNote(ReviewPartnerRegistrationCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private String resolveNextAction(
            boolean emailVerified,
            ApprovalRequestStatus approvalStatus,
            AccountStatus accountStatus
    ) {
        if (!emailVerified) {
            return NEXT_VERIFY_EMAIL;
        }
        if (ApprovalRequestStatus.REJECTED.equals(approvalStatus)) {
            return "REVIEW_REJECTED";
        }
        if (ApprovalRequestStatus.APPROVED.equals(approvalStatus) && AccountStatus.ACTIVE.equals(accountStatus)) {
            return "ACCESS_PARTNER_PORTAL";
        }
        return "WAIT_FOR_REVIEW";
    }

    private void scheduleReviewOutcome(
            ApprovalRequest approvalRequest,
            ProvisionedAccountResult applicant,
            boolean approved
    ) {
        String fullName = approvalRequest.getRequestData().get("fullName");
        String email = applicant.account().email();
        TransactionalEvents.runAfterCommit(() -> {
            sendReviewEmailSafely(email, fullName, approvalRequest.getNote(), approved);
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
        if (notificationPortIn == null) {
            return;
        }
        try {
            notificationPortIn.sendWebNotification(new SendNotificationCommand(
                    accountId,
                    type,
                    title,
                    message,
                    "/partner/application-status",
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
        return new PartnerRegistrationResult(
                approvalRequest.getApprovalRequestId(),
                data.get("organizationCode"),
                data.get("organizationName"),
                data.get("representativeName"),
                data.get("email"),
                data.get("phoneNumber"),
                approvalRequest.getStatus(),
                approvalRequest.getNote(),
                approvalRequest.getCreatedAt()
        );
    }
}
