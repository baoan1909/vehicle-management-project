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
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ChatConversationRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.EmployeeRosterRuleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ParkingLotWorkQueueRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
    private final ChatConversationRepository chatConversationRepository;
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
            ChatConversationRepository chatConversationRepository,
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
        this.chatConversationRepository = chatConversationRepository;
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
            UUID accountId, AccountStatus previousStatus, Instant requestedAt, Instant scheduledAt) {
        AccountDeletionRequestEntity entity = new AccountDeletionRequestEntity();
        entity.setRequestId(UUID.randomUUID());
        entity.setAccountId(accountId);
        entity.setStatus(AccountDeletionRequestStatus.GRACE_PERIOD);
        entity.setPreviousAccountStatus(previousStatus);
        entity.setRequestedAt(requestedAt);
        entity.setScheduledDeletionAt(scheduledAt);
        entity.setAttemptCount(0);
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
    public List<AccountDeletionBlocker> findDeletionBlockers(UUID accountId) {
        if (isDeletedAccount(accountId)) return List.of();
        List<AccountDeletionBlocker> blockers = new ArrayList<>();
        customerRepository.findDeletionCustomerIdByAccountId(accountId)
                .ifPresent(customerId -> addCustomerBlockers(blockers, customerId));
        addOrganizationGovernanceBlockers(blockers, accountId);
        addManagementAssignmentBlockers(blockers, accountId);
        employeeRepository.findDeletionEmployeeIdByAccountId(accountId)
                .ifPresent(employeeId -> addWorkforceBlockers(blockers, employeeId));
        addOperationalOwnershipBlockers(blockers, accountId);
        return List.copyOf(blockers);
    }

    private void addCustomerBlockers(List<AccountDeletionBlocker> blockers, UUID customerId) {
        addIfPresent(parkingSessionRepository.findDeletionBlockingSessionId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_PARKING_SESSION", "Cần kết thúc phiên gửi xe đang mở.", "PARKING_SESSION", id)));
        addIfPresent(subscriptionRepository.findDeletionBlockingSubscriptionId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_SUBSCRIPTION", "Cần kết thúc đăng ký gửi xe đang hoạt động.", "SUBSCRIPTION", id)));
        addIfPresent(invoiceRepository.findDeletionBlockingInvoiceId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("UNPAID_INVOICE", "Cần thanh toán hóa đơn chưa hoàn tất.", "INVOICE", id)));
        addIfPresent(paymentRepository.findDeletionBlockingPaymentId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("PENDING_PAYMENT", "Cần hoàn tất giao dịch thanh toán đang xử lý.", "PAYMENT", id)));
        addIfPresent(lostCardReportRepository.findDeletionBlockingReportId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("OPEN_LOST_CARD_REPORT", "Cần xử lý báo mất thẻ đang mở.", "LOST_CARD_REPORT", id)));
        addIfPresent(walletTopupOrderRepository.findDeletionBlockingTopupId(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("PENDING_WALLET_TOPUP", "Cần hoàn tất lệnh nạp ví đang xử lý.", "WALLET_TOPUP", id)));
    }

    private void addOrganizationGovernanceBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        organizationMembershipRepository.findDeletionGovernanceBlockers(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("LAST_ORGANIZATION_ADMIN",
                        "Bạn cần chỉ định một tài khoản khác có quyền quản trị tổ chức trước khi xóa tài khoản.",
                        "ORGANIZATION_MEMBERSHIP", row.getMembershipId(), row.getOrganizationId(), null,
                        "ASSIGN_ORGANIZATION_ADMIN", "/internal/organizations/" + row.getOrganizationId() + "/members")));
    }

    private void addManagementAssignmentBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        managementAssignmentRepository.findDeletionBlockingAssignments(accountId).forEach(row -> {
            String code = "PENDING_ACCEPTANCE".equals(row.getStatus())
                    ? "PENDING_MANAGEMENT_ACCEPTANCE" : "ACTIVE_MANAGEMENT_ASSIGNMENT";
            blockers.add(new AccountDeletionBlocker(code, "Cần bàn giao hoặc kết thúc phân công quản lý bãi xe.",
                    "MANAGEMENT_ASSIGNMENT", row.getAssignmentId(), row.getOrganizationId(), row.getParkingLotId(),
                    "HANDOVER_MANAGEMENT", "/internal/parking-lots/" + row.getParkingLotId() + "/management"));
        });
    }

    private void addWorkforceBlockers(List<AccountDeletionBlocker> blockers, UUID employeeId) {
        employeeLotAssignmentRepository.findDeletionBlockingAssignments(employeeId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("ACTIVE_EMPLOYEE_LOT_ASSIGNMENT",
                        "Cần kết thúc phân công nhân sự tại bãi xe.", "EMPLOYEE_LOT_ASSIGNMENT",
                        row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(), "END_EMPLOYEE_ASSIGNMENT",
                        "/internal/parking-lots/" + row.getParkingLotId() + "/employees")));
        shiftAssignmentRepository.findDeletionBlockingAssignments(employeeId).forEach(row -> {
            String code = "OPEN".equals(row.getShiftStatus())
                    ? "OPEN_SHIFT_PARTICIPATION" : "ACTIVE_OR_FUTURE_SHIFT_ASSIGNMENT";
            blockers.add(new AccountDeletionBlocker(code, "Cần gỡ hoặc bàn giao ca làm việc.",
                    "SHIFT_ASSIGNMENT", row.getAssignmentId(), null, row.getParkingLotId(), "REASSIGN_SHIFT",
                    "/internal/parking-lots/" + row.getParkingLotId() + "/shifts"));
        });
        rosterRuleRepository.findDeletionBlockingRosterRules(employeeId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("ACTIVE_ROSTER_RULE", "Cần vô hiệu lịch phân ca tự động.",
                        "ROSTER_RULE", row.getResourceId(), null, row.getParkingLotId(), "DEACTIVATE_ROSTER",
                        "/internal/parking-lots/" + row.getParkingLotId() + "/roster")));
    }

    private void addOperationalOwnershipBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        supportTicketRepository.findDeletionBlockingTicketIds(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("OPEN_SUPPORT_TICKET_ASSIGNMENT",
                        "Cần bàn giao phiếu hỗ trợ đang mở.", "SUPPORT_TICKET", id)));
        workQueueRepository.findDeletionBlockingItems(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("OPEN_WORK_QUEUE_ASSIGNMENT", "Cần bàn giao công việc đang mở.",
                        "WORK_QUEUE_ITEM", row.getResourceId(), row.getOrganizationId(), row.getParkingLotId(),
                        "REASSIGN_WORK_ITEM", "/internal/parking-lots/" + row.getParkingLotId() + "/work-queue")));
        chatConversationRepository.findDeletionBlockingOwnershipIds(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_CHAT_OWNERSHIP",
                        "Cần bàn giao hội thoại đang hoạt động.", "CHAT_CONVERSATION", id)));
        shiftRepository.findDeletionBlockingResponsibilities(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("OPEN_SHIFT_RESPONSIBILITY", "Cần bàn giao trách nhiệm ca đang mở.",
                        "SHIFT", row.getResourceId(), null, row.getParkingLotId(), "HANDOVER_OPEN_SHIFT",
                        "/internal/parking-lots/" + row.getParkingLotId() + "/shifts")));
        approvalRequestRepository.findDeletionBlockingOperationalApprovalIds(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("PROCESSING_OPERATIONAL_APPROVAL",
                        "Cần hoàn tất hoặc hủy yêu cầu nghiệp vụ đang chờ duyệt.", "APPROVAL_REQUEST", id)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt) {
        AccountEntity account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalStateException("Account no longer exists"));
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
                entity.getPreviousAccountStatus());
    }

    private static void addIfPresent(Optional<UUID> value, Consumer<UUID> consumer) {
        value.ifPresent(consumer);
    }
}
