package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.operations.approvalrequest.authorization.CustomerOnboardingApprovalAccessGuard;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CustomerOnboardingApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalCandidate;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.CustomerOnboardingApprovalPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.CustomerOnboardingApprovalPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.IdentityProviderAdminPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.SystemAccountIdPortOut;
import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.ApprovalRequestPolicy;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customer.policy.CustomerPolicy;
import com.ban.vehicle_management.infrastructure.mail.VehicleMailService;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.OnboardingApprovalPolicyType;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerOnboardingApprovalUseCaseImpl implements CustomerOnboardingApprovalPortIn {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerOnboardingApprovalUseCaseImpl.class);

    private final CustomerOnboardingApprovalAccessGuard customerOnboardingApprovalAccessGuard;
    private final CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut;
    private final IdentityProviderAdminPortOut identityProviderAdminPortOut;
    private final SystemAccountIdPortOut systemAccountIdPortOut;
    private final OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator;
    private final VehicleMailService vehicleMailService;
    private final NotificationPortIn notificationPortIn;
    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();
    private final CustomerPolicy customerPolicy = new CustomerPolicy();
    private final Clock clock;

    public CustomerOnboardingApprovalUseCaseImpl(
            CustomerOnboardingApprovalAccessGuard customerOnboardingApprovalAccessGuard,
            CustomerOnboardingApprovalPortOut customerOnboardingApprovalPortOut,
            IdentityProviderAdminPortOut identityProviderAdminPortOut,
            SystemAccountIdPortOut systemAccountIdPortOut,
            OnboardingApprovalPolicyEvaluator onboardingApprovalPolicyEvaluator,
            VehicleMailService vehicleMailService,
            NotificationPortIn notificationPortIn
    ) {
        this.customerOnboardingApprovalAccessGuard = customerOnboardingApprovalAccessGuard;
        this.customerOnboardingApprovalPortOut = customerOnboardingApprovalPortOut;
        this.identityProviderAdminPortOut = identityProviderAdminPortOut;
        this.systemAccountIdPortOut = systemAccountIdPortOut;
        this.onboardingApprovalPolicyEvaluator = onboardingApprovalPolicyEvaluator;
        this.vehicleMailService = vehicleMailService;
        this.notificationPortIn = notificationPortIn;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOnboardingApprovalResult> getCustomerOnboardingApprovals(
            CustomerOnboardingApprovalFilterCommand command
    ) {
        customerOnboardingApprovalAccessGuard.requireReadAccess();
        return customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalRequests(
                normalizeFilterCommand(command)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOnboardingApprovalResult getCustomerOnboardingApprovalById(UUID approvalRequestId) {
        customerOnboardingApprovalAccessGuard.requireReadAccess();
        return customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOnboardingApprovalResult getMyLatestCustomerOnboardingApproval() {
        CurrentAccountAccess currentAccount = customerOnboardingApprovalAccessGuard.requireCurrentCustomer();
        return customerOnboardingApprovalPortOut.findLatestCustomerOnboardingApprovalResultByAccountId(currentAccount.accountId())
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
    }

    @Override
    @Transactional
    public CustomerOnboardingApprovalResult approveCustomerOnboardingApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        CurrentAccountAccess currentAccount = customerOnboardingApprovalAccessGuard.requireWriteAccess();
        ApprovalRequest approvalRequest = loadForUpdate(approvalRequestId);
        ensurePending(approvalRequest);
        CustomerOnboardingApprovalCandidate candidate = loadCandidate(approvalRequest);
        return approve(
                approvalRequest,
                candidate,
                currentAccount.accountId(),
                normalizeNote(command),
                Map.of("decisionMode", "MANUAL")
        );
    }

    @Override
    @Transactional
    public Optional<CustomerOnboardingApprovalResult> tryAutoApproveCustomerOnboardingApproval(
            UUID approvalRequestId
    ) {
        ApprovalRequest approvalRequest = loadForUpdate(approvalRequestId);
        if (!ApprovalRequestStatus.PENDING.equals(approvalRequest.getStatus())) {
            return Optional.empty();
        }
        Optional<OnboardingApprovalPolicy> eligiblePolicy = onboardingApprovalPolicyEvaluator.findEligiblePolicy(
                OnboardingApprovalPolicyType.CUSTOMER_ONBOARDING,
                approvalRequest.getCreatedAt()
        );
        if (eligiblePolicy.isEmpty()) {
            return Optional.empty();
        }

        CustomerOnboardingApprovalCandidate candidate = loadCandidate(approvalRequest);
        if (candidate.keycloakUserId() == null
                || !identityProviderAdminPortOut.isEmailVerified(candidate.keycloakUserId())) {
            return Optional.empty();
        }
        UUID systemAccountId = systemAccountIdPortOut.getSystemAccountId();
        return Optional.of(approve(
                approvalRequest,
                candidate,
                systemAccountId,
                "Automatically approved by onboarding policy",
                autoDecisionData(eligiblePolicy.get())
        ));
    }

    @Override
    @Transactional
    public CustomerOnboardingApprovalResult rejectCustomerOnboardingApproval(
            UUID approvalRequestId,
            ReviewInternalEmployeeApprovalCommand command
    ) {
        customerOnboardingApprovalAccessGuard.requireWriteAccess();

        ApprovalRequest approvalRequest = loadForUpdate(approvalRequestId);
        ensurePending(approvalRequest);
        CustomerOnboardingApprovalCandidate candidate = loadCandidate(approvalRequest);

        Customer customer = loadCustomer(candidate.customerId());
        approvalRequestPolicy.reject(approvalRequest, normalizeNote(command));
        customerPolicy.reject(customer);
        customerOnboardingApprovalPortOut.saveCustomerOnboardingApprovalDecision(approvalRequest, customer);

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortOut.findCustomerOnboardingApprovalResultById(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
        TransactionalEvents.runAfterCommit(() -> {
            notifyApprovalResultSafely(
                    result,
                    NotificationType.CUSTOMER_ONBOARDING_REJECTED,
                    "Hồ sơ khách hàng bị từ chối",
                    "Hồ sơ khách hàng của bạn chưa được duyệt."
            );
            sendOnboardingRejectedEmailSafely(result);
        });
        return result;
    }

    @Override
    @Transactional
    public CustomerOnboardingApprovalResult resubmitMyCustomerOnboardingApproval() {
        CurrentAccountAccess currentAccount = customerOnboardingApprovalAccessGuard.requireCurrentCustomer();
        CustomerOnboardingApprovalCandidate candidate = customerOnboardingApprovalPortOut
                .findCandidateByAccountId(currentAccount.accountId())
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval target not found"));

        if (customerOnboardingApprovalPortOut.existsPendingCustomerOnboardingApprovalForCustomer(candidate.customerId())) {
            throw new ConflictException("A customer onboarding approval request is already pending");
        }

        ApprovalRequest latestApprovalRequest = customerOnboardingApprovalPortOut
                .findLatestCustomerOnboardingApprovalRequest(candidate.customerId())
                .orElse(null);
        if (latestApprovalRequest != null && ApprovalRequestStatus.APPROVED.equals(latestApprovalRequest.getStatus())) {
            throw new ConflictException("Customer onboarding approval has already been approved");
        }

        Customer customer = loadCustomer(candidate.customerId());
        customerPolicy.resubmit(customer);
        ApprovalRequest approvalRequest = buildPendingApprovalRequest(candidate.customerId(), currentAccount.accountId());
        customerOnboardingApprovalPortOut.saveCustomerOnboardingApprovalDecision(approvalRequest, customer);

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortOut
                .findCustomerOnboardingApprovalResultById(approvalRequest.getApprovalRequestId())
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
        notifyApprovalResult(result, NotificationType.CUSTOMER_ONBOARDING_RESUBMITTED, "Hồ sơ khách hàng đã gửi lại", "Hồ sơ của bạn đã được gửi lại để duyệt.");
        notifyApprovalReviewers(approvalRequest, NotificationType.CUSTOMER_ONBOARDING_RESUBMITTED, "Có hồ sơ khách hàng gửi lại cần duyệt");
        return result;
    }

    public ApprovalRequest buildPendingApprovalRequest(UUID customerId, UUID requestedBy) {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType(CustomerOnboardingApprovalAccessGuard.REQUEST_TYPE);
        approvalRequest.setTargetSchema(CustomerOnboardingApprovalAccessGuard.TARGET_SCHEMA);
        approvalRequest.setTargetTable(CustomerOnboardingApprovalAccessGuard.TARGET_TABLE);
        approvalRequest.setTargetId(customerId);
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        approvalRequest.setRequestedBy(requestedBy);
        approvalRequestPolicy.initialize(approvalRequest);
        return approvalRequest;
    }

    private CustomerOnboardingApprovalFilterCommand normalizeFilterCommand(
            CustomerOnboardingApprovalFilterCommand command
    ) {
        if (command == null) {
            return new CustomerOnboardingApprovalFilterCommand(null, null);
        }
        return new CustomerOnboardingApprovalFilterCommand(
                TextValidationUtils.normalizeNullableText(command.keyword(), "keyword", 100),
                command.status()
        );
    }

    private String normalizeNote(ReviewInternalEmployeeApprovalCommand command) {
        return TextValidationUtils.normalizeNullableText(command == null ? null : command.note(), "note", 0);
    }

    private Customer loadCustomer(UUID customerId) {
        return customerOnboardingApprovalPortOut.findCustomerById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private ApprovalRequest loadForUpdate(UUID approvalRequestId) {
        return customerOnboardingApprovalPortOut
                .findCustomerOnboardingApprovalRequestByIdForUpdate(approvalRequestId)
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
    }

    private CustomerOnboardingApprovalCandidate loadCandidate(ApprovalRequest approvalRequest) {
        return customerOnboardingApprovalPortOut
                .findCandidateByCustomerId(approvalRequest.getTargetId())
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval target not found"));
    }

    private void ensurePending(ApprovalRequest approvalRequest) {
        if (!ApprovalRequestStatus.PENDING.equals(approvalRequest.getStatus())) {
            throw new ConflictException("Customer onboarding approval request has already been reviewed");
        }
    }

    private CustomerOnboardingApprovalResult approve(
            ApprovalRequest approvalRequest,
            CustomerOnboardingApprovalCandidate candidate,
            UUID actorAccountId,
            String note,
            Map<String, String> decisionData
    ) {
        Customer customer = loadCustomer(candidate.customerId());
        approvalRequestPolicy.approve(approvalRequest, actorAccountId, Instant.now(clock), note);
        approvalRequest.setDecisionData(decisionData);
        customerPolicy.approve(customer, actorAccountId, approvalRequest.getApprovedAt());
        customerOnboardingApprovalPortOut.saveCustomerOnboardingApprovalDecision(approvalRequest, customer);
        String keycloakUserId = customerOnboardingApprovalPortOut.activateCustomerAccount(
                candidate.accountId(),
                actorAccountId
        );
        identityProviderAdminPortOut.updateUserEnabled(keycloakUserId, true);

        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortOut
                .findCustomerOnboardingApprovalResultById(approvalRequest.getApprovalRequestId())
                .orElseThrow(() -> new NotFoundException("Customer onboarding approval request not found"));
        TransactionalEvents.runAfterCommit(() -> {
            notifyApprovalResultSafely(
                    result,
                    NotificationType.CUSTOMER_ONBOARDING_APPROVED,
                    "Hồ sơ khách hàng đã được duyệt",
                    "Hồ sơ khách hàng của bạn đã được duyệt."
            );
            sendOnboardingApprovedEmailSafely(result);
        });
        return result;
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

    private void sendOnboardingApprovedEmail(CustomerOnboardingApprovalResult result) {
        vehicleMailService.sendOnboardingApprovedEmail(
                result.account().email(),
                result.profile().fullName(),
                "khách hàng"
        );
    }

    private void sendOnboardingRejectedEmail(CustomerOnboardingApprovalResult result) {
        vehicleMailService.sendOnboardingRejectedEmail(
                result.account().email(),
                result.profile().fullName(),
                "khách hàng",
                result.request().note()
        );
    }

    private void sendOnboardingApprovedEmailSafely(CustomerOnboardingApprovalResult result) {
        try {
            sendOnboardingApprovedEmail(result);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Customer onboarding {} was approved but the result email could not be sent to {}",
                    result.request().approvalRequestId(),
                    result.account().email(),
                    exception
            );
        }
    }

    private void sendOnboardingRejectedEmailSafely(CustomerOnboardingApprovalResult result) {
        try {
            sendOnboardingRejectedEmail(result);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Customer onboarding {} was rejected but the result email could not be sent to {}",
                    result.request().approvalRequestId(),
                    result.account().email(),
                    exception
            );
        }
    }

    private void notifyApprovalResultSafely(
            CustomerOnboardingApprovalResult result,
            NotificationType notificationType,
            String title,
            String message
    ) {
        try {
            notifyApprovalResult(result, notificationType, title, message);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Customer onboarding {} completed but its web notification could not be sent to account {}",
                    result.request().approvalRequestId(),
                    result.account().accountId(),
                    exception
            );
        }
    }

    private void notifyApprovalResult(
            CustomerOnboardingApprovalResult result,
            NotificationType notificationType,
            String title,
            String message
    ) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                result.account().accountId(),
                notificationType,
                title,
                message,
                "/customer/profile",
                "people",
                "customers",
                result.customer().customerId()
        ));
    }

    private void notifyApprovalReviewers(
            ApprovalRequest approvalRequest,
            NotificationType notificationType,
            String title
    ) {
        ApprovalNotificationSupport.notifyApprovers(
                notificationPortIn,
                approvalRequest,
                null,
                notificationType,
                title
        );
    }
}
