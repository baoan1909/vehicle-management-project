package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.policy.AccountDeletionTombstones;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionEligibilityQueryRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionLifecycleRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionTombstoneRepository;
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
    private final AccountDeletionLifecycleRepository lifecycleRepository;
    private final AccountDeletionEligibilityQueryRepository eligibilityRepository;
    private final AccountDeletionTombstoneRepository tombstoneRepository;

    public AccountDeletionPersistenceAdapter(
            AccountDeletionRequestRepository requestRepository,
            AccountDeletionLifecycleRepository lifecycleRepository,
            AccountDeletionEligibilityQueryRepository eligibilityRepository,
            AccountDeletionTombstoneRepository tombstoneRepository
    ) {
        this.requestRepository = requestRepository;
        this.lifecycleRepository = lifecycleRepository;
        this.eligibilityRepository = eligibilityRepository;
        this.tombstoneRepository = tombstoneRepository;
    }

    @Override
    public AccountStatus lockAccount(UUID accountId) {
        return findAccountForUpdate(accountId).getStatus();
    }

    @Override
    public boolean isEligibleAccount(UUID accountId) {
        return eligibilityRepository.isEligibleAccount(accountId);
    }

    @Override
    public boolean isDeletedAccount(UUID accountId) {
        return lifecycleRepository.findAccount(accountId)
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
        lifecycleRepository.appendStatusHistory(accountId, previousStatus, AccountStatus.PENDING_DELETION,
                "Self-service account deletion requested", Instant.now());
        lifecycleRepository.flush();
    }

    @Override
    public AccountDeletionRequest cancel(UUID requestId, Instant cancelledAt, String reason) {
        AccountDeletionRequestEntity request = requestRepository.findById(requestId).orElseThrow();
        AccountEntity account = findAccountForUpdate(request.getAccountId());
        if (account.getStatus() == AccountStatus.PENDING_DELETION) {
            account.setStatus(request.getPreviousAccountStatus());
            lifecycleRepository.appendStatusHistory(request.getAccountId(), AccountStatus.PENDING_DELETION,
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
        return lifecycleRepository.claimDue(now, leaseUntil, claimToken, batchSize);
    }

    @Override
    public Optional<AccountDeletionRequest> findClaimed(UUID requestId, String claimToken) {
        return requestRepository.findByRequestIdAndClaimToken(requestId, claimToken).map(this::toDomain);
    }

    @Override
    public String findKeycloakUserId(UUID accountId) {
        return lifecycleRepository.findAccount(accountId).map(AccountEntity::getKeycloakUserId).orElse(null);
    }

    @Override
    public List<AccountDeletionBlocker> findDeletionBlockers(UUID accountId) {
        if (isDeletedAccount(accountId)) return List.of();
        List<AccountDeletionBlocker> blockers = new ArrayList<>();
        eligibilityRepository.findCustomerIdByAccountId(accountId)
                .ifPresent(customerId -> addCustomerBlockers(blockers, customerId));
        addOrganizationGovernanceBlockers(blockers, accountId);
        addManagementAssignmentBlockers(blockers, accountId);
        eligibilityRepository.findEmployeeIdByAccountId(accountId)
                .ifPresent(employeeId -> addWorkforceBlockers(blockers, employeeId));
        addOperationalOwnershipBlockers(blockers, accountId);
        return List.copyOf(blockers);
    }

    private void addCustomerBlockers(List<AccountDeletionBlocker> blockers, UUID customerId) {
        addIfPresent(eligibilityRepository.findActiveParkingSession(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_PARKING_SESSION", "Cần kết thúc phiên gửi xe đang mở.", "PARKING_SESSION", id)));
        addIfPresent(eligibilityRepository.findActiveSubscription(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_SUBSCRIPTION", "Cần kết thúc đăng ký gửi xe đang hoạt động.", "SUBSCRIPTION", id)));
        addIfPresent(eligibilityRepository.findUnpaidInvoice(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("UNPAID_INVOICE", "Cần thanh toán hóa đơn chưa hoàn tất.", "INVOICE", id)));
        addIfPresent(eligibilityRepository.findPendingPayment(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("PENDING_PAYMENT", "Cần hoàn tất giao dịch thanh toán đang xử lý.", "PAYMENT", id)));
        addIfPresent(eligibilityRepository.findOpenLostCardReport(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("OPEN_LOST_CARD_REPORT", "Cần xử lý báo mất thẻ đang mở.", "LOST_CARD_REPORT", id)));
        addIfPresent(eligibilityRepository.findPendingWalletTopup(customerId), id -> blockers.add(
                AccountDeletionBlocker.of("PENDING_WALLET_TOPUP", "Cần hoàn tất lệnh nạp ví đang xử lý.", "WALLET_TOPUP", id)));
    }

    private void addOrganizationGovernanceBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        eligibilityRepository.findOrganizationsWithoutSuccessor(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("LAST_ORGANIZATION_ADMIN",
                        "Bạn cần chỉ định một tài khoản khác có quyền quản trị tổ chức trước khi xóa tài khoản.",
                        "ORGANIZATION_MEMBERSHIP", row.membershipId(), row.organizationId(), null,
                        "ASSIGN_ORGANIZATION_ADMIN", "/internal/organizations/" + row.organizationId() + "/members")));
    }

    private void addManagementAssignmentBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        eligibilityRepository.findManagementAssignments(accountId).forEach(row -> {
            String code = "PENDING_ACCEPTANCE".equals(row.status())
                    ? "PENDING_MANAGEMENT_ACCEPTANCE" : "ACTIVE_MANAGEMENT_ASSIGNMENT";
            blockers.add(new AccountDeletionBlocker(code, "Cần bàn giao hoặc kết thúc phân công quản lý bãi xe.",
                    "MANAGEMENT_ASSIGNMENT", row.assignmentId(), row.organizationId(), row.parkingLotId(),
                    "HANDOVER_MANAGEMENT", "/internal/parking-lots/" + row.parkingLotId() + "/management"));
        });
    }

    private void addWorkforceBlockers(List<AccountDeletionBlocker> blockers, UUID employeeId) {
        eligibilityRepository.findEmployeeLotAssignments(employeeId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("ACTIVE_EMPLOYEE_LOT_ASSIGNMENT",
                        "Cần kết thúc phân công nhân sự tại bãi xe.", "EMPLOYEE_LOT_ASSIGNMENT",
                        row.resourceId(), row.organizationId(), row.parkingLotId(), "END_EMPLOYEE_ASSIGNMENT",
                        "/internal/parking-lots/" + row.parkingLotId() + "/employees")));
        eligibilityRepository.findShiftAssignments(employeeId).forEach(row -> {
            String code = "OPEN".equals(row.shiftStatus())
                    ? "OPEN_SHIFT_PARTICIPATION" : "ACTIVE_OR_FUTURE_SHIFT_ASSIGNMENT";
            blockers.add(new AccountDeletionBlocker(code, "Cần gỡ hoặc bàn giao ca làm việc.",
                    "SHIFT_ASSIGNMENT", row.assignmentId(), null, row.parkingLotId(), "REASSIGN_SHIFT",
                    "/internal/parking-lots/" + row.parkingLotId() + "/shifts"));
        });
        eligibilityRepository.findActiveRosterRules(employeeId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("ACTIVE_ROSTER_RULE", "Cần vô hiệu lịch phân ca tự động.",
                        "ROSTER_RULE", row.resourceId(), null, row.parkingLotId(), "DEACTIVATE_ROSTER",
                        "/internal/parking-lots/" + row.parkingLotId() + "/roster")));
    }

    private void addOperationalOwnershipBlockers(List<AccountDeletionBlocker> blockers, UUID accountId) {
        eligibilityRepository.findOpenSupportTickets(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("OPEN_SUPPORT_TICKET_ASSIGNMENT",
                        "Cần bàn giao phiếu hỗ trợ đang mở.", "SUPPORT_TICKET", id)));
        eligibilityRepository.findOpenWorkItems(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("OPEN_WORK_QUEUE_ASSIGNMENT", "Cần bàn giao công việc đang mở.",
                        "WORK_QUEUE_ITEM", row.resourceId(), row.organizationId(), row.parkingLotId(),
                        "REASSIGN_WORK_ITEM", "/internal/parking-lots/" + row.parkingLotId() + "/work-queue")));
        eligibilityRepository.findActiveChatOwnerships(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("ACTIVE_CHAT_OWNERSHIP",
                        "Cần bàn giao hội thoại đang hoạt động.", "CHAT_CONVERSATION", id)));
        eligibilityRepository.findOpenShiftResponsibilities(accountId).forEach(row -> blockers.add(
                new AccountDeletionBlocker("OPEN_SHIFT_RESPONSIBILITY", "Cần bàn giao trách nhiệm ca đang mở.",
                        "SHIFT", row.resourceId(), null, row.parkingLotId(), "HANDOVER_OPEN_SHIFT",
                        "/internal/parking-lots/" + row.parkingLotId() + "/shifts")));
        eligibilityRepository.findProcessingOperationalApprovals(accountId).forEach(id -> blockers.add(
                AccountDeletionBlocker.of("PROCESSING_OPERATIONAL_APPROVAL",
                        "Cần hoàn tất hoặc hủy yêu cầu nghiệp vụ đang chờ duyệt.", "APPROVAL_REQUEST", id)));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt) {
        AccountEntity account = lifecycleRepository.findAccountForUpdate(accountId)
                .orElseThrow(() -> new IllegalStateException("Account no longer exists"));
        UUID profileId = account.getUserProfileId();
        List<String> objectKeys = tombstoneRepository.findAvatarObjectKeys(profileId);
        if (account.getStatus() == AccountStatus.DELETED) return List.copyOf(objectKeys);
        String marker = AccountDeletionTombstones.marker(accountId);
        tombstoneRepository.tombstoneRelatedData(accountId, profileId, marker, completedAt);
        lifecycleRepository.appendStatusHistory(accountId, account.getStatus(), AccountStatus.DELETED,
                "Account deletion grace period completed", completedAt);
        account.setUsername(marker);
        account.setEmail(AccountDeletionTombstones.email(accountId));
        account.setKeycloakUserId(marker);
        account.setStatus(AccountStatus.DELETED);
        account.setLastLoginAt(null);
        account.setLockedUntil(null);
        account.setFailedLoginCount(0);
        lifecycleRepository.flush();
        return List.copyOf(objectKeys);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeFileCleanup(UUID requestId, UUID accountId, Instant completedAt) {
        lifecycleRepository.findAccount(accountId).ifPresent(account ->
                tombstoneRepository.completeAvatarCleanup(account.getUserProfileId(), completedAt));
        markCompleted(requestId, completedAt);
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
        return lifecycleRepository.findAccountForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản hiện tại"));
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
