package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.policy.AccountDeletionTombstones;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountStatusHistoryEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountStatusHistoryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountIdentityRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.MemberParkingLotScopeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.OrganizationMembershipRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.ParkingLotManagementAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.CustomerRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileAvatarRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.UserProfileRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.parking.ParkingSessionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.SubscriptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.accesscontrol.LostCardReportRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.InvoiceRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletTopupOrderRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ApprovalRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationMemberRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ParkingLotWorkQueueRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.Map;
import com.ban.vehicle_management.infrastructure.persistence.database.projection.iam.AccountDeletionResourceScopeProjection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDeletionPersistenceAdapter implements AccountDeletionPortOut {
    private static final List<AccountDeletionRequestStatus> ACTIVE_STATUSES = List.of(
            AccountDeletionRequestStatus.GRACE_PERIOD,
            AccountDeletionRequestStatus.PROCESSING,
            AccountDeletionRequestStatus.RETRY,
            AccountDeletionRequestStatus.REVIEW_REQUIRED);

    private final AccountDeletionRequestRepository requestRepository;
    private final AccountRepository accountRepository;
    private final AccountStatusHistoryRepository statusHistoryRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ParkingSessionRepository parkingSessionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final LostCardReportRepository lostCardReportRepository;
    private final WalletTopupOrderRepository walletTopupOrderRepository;
    private final OrganizationMembershipRepository organizationMembershipRepository;
    private final ParkingLotManagementAssignmentRepository managementAssignmentRepository;
    private final EmployeeParkingLotAssignmentRepository employeeLotAssignmentRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final EmployeeRosterRuleRepository rosterRuleRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ParkingLotWorkQueueRepository workQueueRepository;
    private final ShiftRepository shiftRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final UserProfileAvatarRepository avatarRepository;
    private final UserProfileRepository userProfileRepository;
    private final MemberParkingLotScopeRepository memberParkingLotScopeRepository;
    private final AccountIdentityRepository accountIdentityRepository;
    private final ChatConversationMemberRepository chatMemberRepository;

    public AccountDeletionPersistenceAdapter(
            AccountDeletionRequestRepository requestRepository,
            AccountRepository accountRepository,
            AccountStatusHistoryRepository statusHistoryRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            ParkingSessionRepository parkingSessionRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            LostCardReportRepository lostCardReportRepository,
            WalletTopupOrderRepository walletTopupOrderRepository,
            OrganizationMembershipRepository organizationMembershipRepository,
            ParkingLotManagementAssignmentRepository managementAssignmentRepository,
            EmployeeParkingLotAssignmentRepository employeeLotAssignmentRepository,
            ShiftAssignmentRepository shiftAssignmentRepository,
            EmployeeRosterRuleRepository rosterRuleRepository,
            SupportTicketRepository supportTicketRepository,
            ParkingLotWorkQueueRepository workQueueRepository,
            ShiftRepository shiftRepository,
            ApprovalRequestRepository approvalRequestRepository,
            UserProfileAvatarRepository avatarRepository,
            UserProfileRepository userProfileRepository,
            MemberParkingLotScopeRepository memberParkingLotScopeRepository,
            AccountIdentityRepository accountIdentityRepository,
            ChatConversationMemberRepository chatMemberRepository
    ) {
        this.requestRepository = requestRepository;
        this.accountRepository = accountRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.lostCardReportRepository = lostCardReportRepository;
        this.walletTopupOrderRepository = walletTopupOrderRepository;
        this.organizationMembershipRepository = organizationMembershipRepository;
        this.managementAssignmentRepository = managementAssignmentRepository;
        this.employeeLotAssignmentRepository = employeeLotAssignmentRepository;
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.rosterRuleRepository = rosterRuleRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.workQueueRepository = workQueueRepository;
        this.shiftRepository = shiftRepository;
        this.approvalRequestRepository = approvalRequestRepository;
        this.avatarRepository = avatarRepository;
        this.userProfileRepository = userProfileRepository;
        this.memberParkingLotScopeRepository = memberParkingLotScopeRepository;
        this.accountIdentityRepository = accountIdentityRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    @Override
    public AccountStatus lockAccount(UUID accountId) {
        return findAccountForUpdate(accountId).getStatus();
    }

    @Override
    public boolean isEligibleAccount(UUID accountId) {
        return accountRepository.isEligibleForDeletion(accountId);
    }

    @Override
    public boolean isDeletedAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(AccountEntity::getStatus)
                .filter(AccountStatus.DELETED::equals)
                .isPresent();
    }

    @Override
    public Optional<AccountDeletionRequest> findActive(UUID accountId, boolean forUpdate) {
        Optional<AccountDeletionRequestEntity> result = forUpdate
                ? requestRepository.findActiveForUpdate(accountId, ACTIVE_STATUSES)
                : requestRepository.findFirstByAccountIdAndStatusInOrderByRequestedAtDesc(accountId, ACTIVE_STATUSES);
        return result.map(this::toDomain);
    }

    @Override
    public AccountDeletionRequest create(
            UUID accountId, AccountStatus previousStatus, Instant requestedAt, Instant scheduledAt,
            boolean activeSubscriptionTerminationWithoutRefundConfirmed) {
        AccountDeletionRequestEntity entity = new AccountDeletionRequestEntity();
        entity.setRequestId(UUID.randomUUID());
        entity.setAccountId(accountId);
        entity.setStatus(AccountDeletionRequestStatus.GRACE_PERIOD);
        entity.setPreviousAccountStatus(previousStatus);
        entity.setRequestedAt(requestedAt);
        entity.setScheduledDeletionAt(scheduledAt);
        entity.setAttemptCount(0);
        entity.setActiveSubscriptionNoRefundConfirmed(activeSubscriptionTerminationWithoutRefundConfirmed);
        return toDomain(requestRepository.saveAndFlush(entity));
    }

    @Override
    public void markPendingDeletion(UUID accountId) {
        AccountEntity account = findAccountForUpdate(accountId);
        if (account.getStatus() == AccountStatus.PENDING_DELETION) return;
        AccountStatus previousStatus = account.getStatus();
        account.setStatus(AccountStatus.PENDING_DELETION);
        appendStatusHistory(accountId, previousStatus, AccountStatus.PENDING_DELETION,
                "Self-service account deletion requested", Instant.now());
        accountRepository.saveAndFlush(account);
    }

    @Override
    public AccountDeletionRequest cancel(UUID requestId, Instant cancelledAt, String reason) {
        AccountDeletionRequestEntity request = requestRepository.findById(requestId).orElseThrow();
        AccountEntity account = findAccountForUpdate(request.getAccountId());
        if (account.getStatus() == AccountStatus.PENDING_DELETION) {
            account.setStatus(request.getPreviousAccountStatus());
            appendStatusHistory(request.getAccountId(), AccountStatus.PENDING_DELETION,
                    request.getPreviousAccountStatus(), "Account deletion request cancelled", cancelledAt);
        }
        request.setStatus(AccountDeletionRequestStatus.CANCELLED);
        request.setCancelledAt(cancelledAt);
        request.setCancellationReason(reason);
        request.setClaimToken(null);
        request.setProcessingLeaseUntil(null);
        request.setNextRetryAt(null);
        request.setLastErrorCode(null);
        request.setLastErrorMessage(null);
        return toDomain(requestRepository.saveAndFlush(request));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimDue(Instant now, Instant leaseUntil, String claimToken, int batchSize) {
        return requestRepository.claimDue(now, leaseUntil, claimToken, batchSize);
    }

    @Override
    public Optional<AccountDeletionRequest> findClaimed(UUID requestId, String claimToken) {
        return requestRepository.findByRequestIdAndClaimToken(requestId, claimToken).map(this::toDomain);
    }

    @Override
    public String findKeycloakUserId(UUID accountId) {
        return accountRepository.findById(accountId).map(AccountEntity::getKeycloakUserId).orElse(null);
    }

    @Override
    public String findIdentityProviderSubjectSnapshot(UUID requestId) {
        return requestRepository.findById(requestId)
                .map(AccountDeletionRequestEntity::getIdentityProviderSubjectSnapshot)
                .orElse(null);
    }

    @Override
    public Optional<UUID> findDeletionCustomerId(UUID accountId) {
        return customerRepository.findDeletionCustomerIdByAccountId(accountId);
    }

    @Override
    public List<AccountDeletionBlocker> findDeletionBlockers(UUID accountId) {
        return findDeletionBlockers(accountId, List.of());
    }

    @Override
    public List<AccountDeletionBlocker> findDeletionBlockers(
            UUID accountId, Collection<UUID> closingOrganizationIds) {
        if (isDeletedAccount(accountId)) return List.of();
        Set<UUID> closingOrganizations = closingOrganizationIds == null
                ? Set.of() : Set.copyOf(closingOrganizationIds);
        List<AccountDeletionBlocker> blockers = new ArrayList<>();
        customerRepository.findDeletionCustomerIdByAccountId(accountId)
                .ifPresent(customerId -> addCustomerBlockers(blockers, customerId));
        addManagementAssignmentBlockers(blockers, accountId, closingOrganizations);
        employeeRepository.findDeletionEmployeeIdByAccountId(accountId)
                .ifPresent(employeeId -> addWorkforceBlockers(blockers, employeeId, closingOrganizations));
        addOperationalOwnershipBlockers(blockers, accountId, closingOrganizations);
        return List.copyOf(blockers);
    }

    private void addCustomerBlockers(List<AccountDeletionBlocker> blockers, UUID customerId) {
        addIfPresent(parkingSessionRepository.findDeletionBlockingSessionId(customerId), id -> blockers.add(
                remediationBlocker("ACTIVE_PARKING_SESSION", "Cần kết thúc phiên gửi xe đang mở.",
                        "PARKING_SESSION", id, null, null, "CLOSE_PARKING_SESSION",
                        "/customer/parking-history?sessionId=" + id)));
        addIfPresent(walletTopupOrderRepository.findDeletionBlockingTopupId(customerId), id -> blockers.add(
                remediationBlocker("PENDING_WALLET_TOPUP", "Cần hoàn tất lệnh nạp ví đang xử lý.",
                        "WALLET_TOPUP", id, null, null, "REVIEW_WALLET_TOPUP",
                        "/customer/wallet/topup-result?topupOrderId=" + id)));
    }

    private void addManagementAssignmentBlockers(
            List<AccountDeletionBlocker> blockers, UUID accountId, Set<UUID> closingOrganizations) {
        managementAssignmentRepository.findDeletionBlockingAssignments(accountId).stream()
                .filter(row -> !closingOrganizations.contains(row.getOrganizationId())).forEach(row -> {
            String code = "PENDING_ACCEPTANCE".equals(row.getStatus())
                    ? "PENDING_MANAGEMENT_ACCEPTANCE" : "ACTIVE_MANAGEMENT_ASSIGNMENT";
            blockers.add(remediationBlocker(code, "Cần bàn giao hoặc kết thúc phân công quản lý bãi xe.",
                    "MANAGEMENT_ASSIGNMENT", row.getAssignmentId(), row.getOrganizationId(), row.getParkingLotId(),
                    "HANDOVER_MANAGEMENT", "/admin/parking-lots?lotId=" + row.getParkingLotId()
                            + "&action=manage-assignments"));
        });
    }

    private void addWorkforceBlockers(
            List<AccountDeletionBlocker> blockers, UUID employeeId, Set<UUID> closingOrganizations) {
        employeeLotAssignmentRepository.findDeletionBlockingAssignments(employeeId).stream()
                .filter(row -> !closingOrganizations.contains(row.getOrganizationId())).forEach(row -> blockers.add(
                remediationBlocker("ACTIVE_EMPLOYEE_LOT_ASSIGNMENT",
                        "Cần kết thúc phân công nhân sự tại bãi xe.", "EMPLOYEE_LOT_ASSIGNMENT",
                        row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(), "END_EMPLOYEE_ASSIGNMENT",
                        "/admin/shifts?workspace=parkingAssignments&parkingLotId=" + row.getParkingLotId())));
        shiftAssignmentRepository.findDeletionBlockingAssignments(employeeId).stream()
                .filter(row -> outsideClosingOrganizations(row.getOrganizationId(), closingOrganizations))
                .forEach(row -> {
            String code = "OPEN".equals(row.getShiftStatus())
                    ? "OPEN_SHIFT_PARTICIPATION" : "ACTIVE_OR_FUTURE_SHIFT_ASSIGNMENT";
            blockers.add(remediationBlocker(code, "Cần gỡ hoặc bàn giao ca làm việc.",
                    "SHIFT_ASSIGNMENT", row.getAssignmentId(), row.getOrganizationId(), row.getParkingLotId(),
                    "REASSIGN_SHIFT",
                    "/admin/shifts?workspace=schedule&parkingLotId=" + row.getParkingLotId()
                            + "&assignmentId=" + row.getAssignmentId()));
        });
        rosterRuleRepository.findDeletionBlockingRosterRules(employeeId).stream()
                .filter(row -> outsideClosingOrganizations(row.getOrganizationId(), closingOrganizations))
                .forEach(row -> blockers.add(remediationBlocker(
                        "ACTIVE_ROSTER_RULE", "Cần vô hiệu lịch phân ca tự động.",
                        "ROSTER_RULE", row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                        "DEACTIVATE_ROSTER", "/admin/shifts?workspace=rules&parkingLotId=" + row.getParkingLotId()
                                + "&rosterRuleId=" + row.getResourceId())));
    }

    private void addOperationalOwnershipBlockers(
            List<AccountDeletionBlocker> blockers, UUID accountId, Set<UUID> closingOrganizations) {
        resourcesRequiringHandover(
                supportTicketRepository.findDeletionBlockingTicketScopes(accountId), closingOrganizations)
                .forEach(row -> blockers.add(remediationBlocker("OPEN_SUPPORT_TICKET_ASSIGNMENT",
                        "Cần bàn giao phiếu hỗ trợ đang mở.", "SUPPORT_TICKET", row.getResourceId(),
                        row.getOrganizationId(), row.getParkingLotId(), "HANDOVER_SUPPORT_TICKET",
                        "/admin/support-tickets?ticketId=" + row.getResourceId())));
        workQueueRepository.findDeletionBlockingItems(accountId).stream()
                .filter(row -> !closingOrganizations.contains(row.getOrganizationId())).forEach(row -> blockers.add(
                remediationBlocker("OPEN_WORK_QUEUE_ASSIGNMENT", "Cần bàn giao công việc đang mở.",
                        "WORK_QUEUE_ITEM", row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                        "REASSIGN_WORK_ITEM", "/admin/support-center?workQueueItemId=" + row.getResourceId())));
        shiftRepository.findDeletionBlockingResponsibilities(accountId).stream()
                .filter(row -> outsideClosingOrganizations(row.getOrganizationId(), closingOrganizations))
                .forEach(row -> blockers.add(remediationBlocker(
                        "OPEN_SHIFT_RESPONSIBILITY", "Cần bàn giao trách nhiệm ca đang mở.",
                        "SHIFT", row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                        "HANDOVER_OPEN_SHIFT", "/admin/shifts?workspace=schedule&parkingLotId="
                                + row.getParkingLotId() + "&shiftId=" + row.getResourceId())));
        resourcesRequiringHandover(
                approvalRequestRepository.findDeletionBlockingOperationalApprovalScopes(accountId),
                closingOrganizations).forEach(row -> blockers.add(remediationBlocker(
                        "PENDING_OPERATIONAL_APPROVAL",
                        "Cần hoàn tất hoặc hủy yêu cầu nghiệp vụ đang chờ duyệt.", "APPROVAL_REQUEST",
                        row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                        "RESOLVE_APPROVAL_REQUEST",
                        "/admin/account?tab=onboarding&approvalRequestId=" + row.getResourceId())));
    }

    private static boolean outsideClosingOrganizations(UUID organizationId, Set<UUID> closingOrganizations) {
        return organizationId == null || !closingOrganizations.contains(organizationId);
    }

    private static List<AccountDeletionResourceScopeProjection> resourcesRequiringHandover(
            List<AccountDeletionResourceScopeProjection> rows, Set<UUID> closingOrganizations) {
        Map<UUID, List<AccountDeletionResourceScopeProjection>> rowsByResource = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        AccountDeletionResourceScopeProjection::getResourceId));
        return rowsByResource.values().stream()
                .filter(scopes -> scopes.stream().anyMatch(
                        scope -> outsideClosingOrganizations(scope.getOrganizationId(), closingOrganizations)))
                .map(scopes -> scopes.stream()
                        .filter(scope -> outsideClosingOrganizations(
                                scope.getOrganizationId(), closingOrganizations))
                        .findFirst().orElseThrow())
                .toList();
    }

    private static AccountDeletionBlocker remediationBlocker(
            String code,
            String message,
            String resourceType,
            UUID resourceId,
            UUID organizationId,
            UUID parkingLotId,
            String remediationAction,
            String remediationUrl) {
        return new AccountDeletionBlocker(code, message, resourceType, resourceId, organizationId, parkingLotId,
                remediationAction, remediationUrl);
    }

    @Override
    @Transactional
    public List<String> completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt) {
        AccountEntity account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalStateException("Account no longer exists"));
        AccountDeletionRequestEntity request = requestRepository.findById(requestId).orElseThrow();
        if (request.getIdentityProviderSubjectSnapshot() == null
                && account.getKeycloakUserId() != null
                && !account.getKeycloakUserId().startsWith("_deleted_")) {
            request.setIdentityProviderSubjectSnapshot(account.getKeycloakUserId());
            requestRepository.save(request);
        }
        UUID profileId = account.getUserProfileId();
        List<String> objectKeys = profileId == null
                ? List.of()
                : avatarRepository.findDeletionObjectKeys(profileId);
        if (account.getStatus() == AccountStatus.DELETED) return List.copyOf(objectKeys);
        String marker = AccountDeletionTombstones.marker(accountId);
        tombstoneRelatedData(accountId, profileId, marker, completedAt);
        appendStatusHistory(accountId, account.getStatus(), AccountStatus.DELETED,
                "Account deletion grace period completed", completedAt);
        account.setUsername(marker);
        account.setEmail(AccountDeletionTombstones.email(accountId));
        account.setKeycloakUserId(marker);
        account.setStatus(AccountStatus.DELETED);
        account.setLastLoginAt(null);
        account.setLockedUntil(null);
        account.setFailedLoginCount(0);
        accountRepository.saveAndFlush(account);
        return List.copyOf(objectKeys);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFileCleanup(UUID requestId, UUID accountId, Instant completedAt) {
        accountRepository.findById(accountId).map(AccountEntity::getUserProfileId)
                .filter(java.util.Objects::nonNull)
                .ifPresent(profileId -> avatarRepository.completeDeletionCleanup(profileId, completedAt));
        markCompleted(requestId, completedAt);
    }

    private void tombstoneRelatedData(UUID accountId, UUID profileId, String marker, Instant now) {
        if (profileId != null) {
            customerRepository.tombstoneByUserProfileId(profileId, marker, now);
            employeeRepository.tombstoneByUserProfileId(profileId, marker, now);
            avatarRepository.markDeletedByUserProfileId(profileId, now);
            userProfileRepository.tombstoneForAccountDeletion(profileId, marker, now);
        }
        approvalRequestRepository.cancelPendingOnboardingForDeletedAccount(accountId, profileId, now);
        if (profileId != null) {
            approvalRequestRepository.cancelPendingAvatarApprovalsForDeletedAccount(profileId, now);
        }
        memberParkingLotScopeRepository.deleteAllByAccountId(accountId);
        organizationMembershipRepository.deactivateForDeletedAccount(accountId, now);
        accountIdentityRepository.tombstoneProviderSubjects(accountId, marker, now);
        chatMemberRepository.removeAllForDeletedAccount(accountId, now);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID requestId, Instant now, Instant nextRetryAt, int maxAttempts, String code, String message) {
        AccountDeletionRequestEntity entity = requestRepository.findById(requestId).orElseThrow();
        entity.setLastErrorCode(code);
        entity.setLastErrorMessage(message == null ? null : message.substring(0, Math.min(1000, message.length())));
        entity.setClaimToken(null);
        entity.setProcessingLeaseUntil(null);
        if (entity.getAttemptCount() >= maxAttempts) {
            entity.setStatus(AccountDeletionRequestStatus.REVIEW_REQUIRED);
            entity.setNextRetryAt(null);
        } else {
            entity.setStatus(AccountDeletionRequestStatus.RETRY);
            entity.setNextRetryAt(nextRetryAt);
        }
        requestRepository.saveAndFlush(entity);
    }

    @Override
    public void block(UUID requestId, Instant now, String code, String message) {
        AccountDeletionRequestEntity entity = requestRepository.findById(requestId).orElseThrow();
        AccountEntity account = findAccountForUpdate(entity.getAccountId());
        if (account.getStatus() == AccountStatus.PENDING_DELETION) {
            account.setStatus(entity.getPreviousAccountStatus());
            appendStatusHistory(entity.getAccountId(), AccountStatus.PENDING_DELETION,
                    entity.getPreviousAccountStatus(), "Account deletion blocked by changed precondition", now);
            accountRepository.saveAndFlush(account);
        }
        entity.setStatus(AccountDeletionRequestStatus.BLOCKED);
        entity.setLastErrorCode(code);
        entity.setLastErrorMessage(message == null ? null : message.substring(0, Math.min(1000, message.length())));
        entity.setClaimToken(null);
        entity.setProcessingLeaseUntil(null);
        entity.setNextRetryAt(null);
        requestRepository.saveAndFlush(entity);
    }

    private AccountEntity findAccountForUpdate(UUID accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản hiện tại"));
    }

    private void appendStatusHistory(
            UUID accountId,
            AccountStatus oldStatus,
            AccountStatus newStatus,
            String reason,
            Instant changedAt) {
        statusHistoryRepository.save(new AccountStatusHistoryEntity(
                UUID.randomUUID(), accountId, null, oldStatus, newStatus, reason, changedAt, null, null));
    }

    private void markCompleted(UUID requestId, Instant completedAt) {
        AccountDeletionRequestEntity request = requestRepository.findById(requestId).orElseThrow();
        request.setStatus(AccountDeletionRequestStatus.COMPLETED);
        request.setCompletedAt(completedAt);
        request.setClaimToken(null);
        request.setProcessingLeaseUntil(null);
        request.setNextRetryAt(null);
        request.setLastErrorCode(null);
        request.setLastErrorMessage(null);
        requestRepository.saveAndFlush(request);
    }

    private AccountDeletionRequest toDomain(AccountDeletionRequestEntity entity) {
        return new AccountDeletionRequest(entity.getRequestId(), entity.getAccountId(), entity.getStatus(),
                entity.getRequestedAt(), entity.getScheduledDeletionAt(), entity.getCancelledAt(),
                entity.getCompletedAt(), entity.getAttemptCount(), entity.getLastErrorCode(),
                entity.getPreviousAccountStatus(), entity.isActiveSubscriptionNoRefundConfirmed());
    }

    private static void addIfPresent(Optional<UUID> value, Consumer<UUID> consumer) {
        value.ifPresent(consumer);
    }
}
