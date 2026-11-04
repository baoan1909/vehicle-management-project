package com.ban.vehicle_management.infrastructure.persistence.adapter.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionRequest;
import com.ban.vehicle_management.domain.iam.account.policy.AccountDeletionTombstones;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountDeletionRequestEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.iam.AccountDeletionRequestRepository;
import com.ban.vehicle_management.shared.enumeration.iam.AccountDeletionRequestStatus;
import com.ban.vehicle_management.shared.enumeration.iam.AccountStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountDeletionPersistenceAdapter implements AccountDeletionPortOut {

    private static final List<AccountDeletionRequestStatus> ACTIVE_STATUSES = List.of(
            AccountDeletionRequestStatus.GRACE_PERIOD,
            AccountDeletionRequestStatus.PROCESSING,
            AccountDeletionRequestStatus.RETRY,
            AccountDeletionRequestStatus.REVIEW_REQUIRED
    );

    private final AccountDeletionRequestRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public AccountDeletionPersistenceAdapter(AccountDeletionRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void lockAccount(UUID accountId) {
        entityManager.find(AccountEntity.class, accountId, LockModeType.PESSIMISTIC_WRITE);
    }

    @Override
    public boolean isActiveCustomerAccount(UUID accountId) {
        Number count = (Number) entityManager.createNativeQuery("""
                select count(*) from iam.accounts account
                join people.user_profiles profile on profile.user_profile_id = account.user_profile_id
                join people.customers customer on customer.user_profile_id = profile.user_profile_id
                where account.account_id = :accountId
                  and account.status = 'ACTIVE'
                  and profile.status <> 'DELETED'
                  and customer.status <> 'DELETED'
                  and customer.approval_status = 'APPROVED'
                """)
                .setParameter("accountId", accountId)
                .getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public boolean isDeletedAccount(UUID accountId) {
        AccountEntity account = entityManager.find(AccountEntity.class, accountId);
        return account != null && account.getStatus() == AccountStatus.DELETED;
    }

    @Override
    public Optional<AccountDeletionRequest> findActive(UUID accountId, boolean forUpdate) {
        Optional<AccountDeletionRequestEntity> result = forUpdate
                ? repository.findActiveForUpdate(accountId, ACTIVE_STATUSES)
                : repository.findFirstByAccountIdAndStatusInOrderByRequestedAtDesc(accountId, ACTIVE_STATUSES);
        return result.map(this::toDomain);
    }

    @Override
    public AccountDeletionRequest create(UUID accountId, Instant requestedAt, Instant scheduledAt) {
        AccountDeletionRequestEntity entity = new AccountDeletionRequestEntity();
        entity.setRequestId(UUID.randomUUID());
        entity.setAccountId(accountId);
        entity.setStatus(AccountDeletionRequestStatus.GRACE_PERIOD);
        entity.setRequestedAt(requestedAt);
        entity.setScheduledDeletionAt(scheduledAt);
        entity.setAttemptCount(0);
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    public AccountDeletionRequest cancel(UUID requestId, Instant cancelledAt, String reason) {
        AccountDeletionRequestEntity entity = repository.findById(requestId).orElseThrow();
        entity.setStatus(AccountDeletionRequestStatus.CANCELLED);
        entity.setCancelledAt(cancelledAt);
        entity.setCancellationReason(reason);
        entity.setClaimToken(null);
        entity.setProcessingLeaseUntil(null);
        return toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @SuppressWarnings("unchecked")
    public List<UUID> claimDue(Instant now, Instant leaseUntil, String claimToken, int batchSize) {
        List<?> values = entityManager.createNativeQuery("""
                update iam.account_deletion_requests request
                   set status = 'PROCESSING', claim_token = :claimToken,
                       processing_started_at = :now, processing_lease_until = :leaseUntil,
                       attempt_count = attempt_count + 1, updated_at = :now,
                       version = version + 1
                 where request.request_id in (
                       select candidate.request_id
                         from iam.account_deletion_requests candidate
                        where ((candidate.status in ('GRACE_PERIOD','RETRY')
                                and candidate.scheduled_deletion_at <= :now
                                and (candidate.next_retry_at is null or candidate.next_retry_at <= :now))
                               or (candidate.status = 'PROCESSING'
                                   and candidate.processing_lease_until <= :now))
                        order by candidate.scheduled_deletion_at, candidate.request_id
                        for update skip locked
                        limit :batchSize
                 )
                returning request.request_id
                """)
                .setParameter("claimToken", claimToken)
                .setParameter("now", now)
                .setParameter("leaseUntil", leaseUntil)
                .setParameter("batchSize", batchSize)
                .getResultList();
        return values.stream().map(value -> value instanceof UUID uuid ? uuid : UUID.fromString(value.toString())).toList();
    }

    @Override
    public Optional<AccountDeletionRequest> findClaimed(UUID requestId, String claimToken) {
        return repository.findByRequestIdAndClaimToken(requestId, claimToken).map(this::toDomain);
    }

    @Override
    public String findKeycloakUserId(UUID accountId) {
        return entityManager.find(AccountEntity.class, accountId).getKeycloakUserId();
    }

    @Override
    public List<String> findDeletionBlockers(UUID accountId) {
        List<String> blockers = new ArrayList<>();
        UUID customerId = findCustomerId(accountId);
        if (customerId == null) {
            blockers.add("ACCOUNT_PROFILE_NOT_ELIGIBLE");
            return blockers;
        }
        addBlocker(blockers, "ACTIVE_PARKING_SESSION", "select exists(select 1 from parking.parking_sessions where customer_id=:id and status in ('OPEN','LOST_CARD'))", customerId);
        addBlocker(blockers, "ACTIVE_SUBSCRIPTION", "select exists(select 1 from access_control.subscriptions where customer_id=:id and status in ('PENDING','PENDING_PAYMENT','PENDING_CARD','ACTIVE'))", customerId);
        addBlocker(blockers, "UNPAID_INVOICE", "select exists(select 1 from billing.invoices where customer_id=:id and status='UNPAID')", customerId);
        addBlocker(blockers, "PENDING_PAYMENT", "select exists(select 1 from billing.payments payment join billing.invoices invoice on invoice.invoice_id=payment.invoice_id where invoice.customer_id=:id and payment.status='PENDING')", customerId);
        addBlocker(blockers, "OPEN_LOST_CARD_REPORT", "select exists(select 1 from access_control.lost_card_reports where customer_id=:id and status='OPEN')", customerId);
        addBlocker(blockers, "PENDING_WALLET_TOPUP", "select exists(select 1 from billing.wallet_topup_orders topup join billing.wallets wallet on wallet.wallet_id=topup.wallet_id where wallet.customer_id=:id and topup.status='PENDING')", customerId);
        return blockers;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeWithTombstones(UUID requestId, UUID accountId, Instant completedAt) {
        AccountEntity account = entityManager.find(AccountEntity.class, accountId, LockModeType.PESSIMISTIC_WRITE);
        if (account == null) {
            throw new IllegalStateException("Account no longer exists");
        }
        if (account.getStatus() == AccountStatus.DELETED) {
            markCompleted(requestId, completedAt);
            return;
        }
        String marker = AccountDeletionTombstones.marker(accountId);
        UUID profileId = account.getUserProfileId();

        if (profileId != null) {
            entityManager.createNativeQuery("""
                    update people.customers set customer_code=:marker, status='DELETED',
                           approved_by=null, updated_at=:now
                     where user_profile_id=:profileId
                    """).setParameter("marker", marker).setParameter("now", completedAt)
                    .setParameter("profileId", profileId).executeUpdate();
            entityManager.createNativeQuery("""
                    update people.user_profiles set full_name=:marker,
                           phone_number=:marker, identify_card=:marker, address_detail=:marker,
                           province_code=null, district_code=null, ward_code=null,
                           address_display=:marker, date_of_birth=null, gender=null,
                           status='DELETED', updated_at=:now
                     where user_profile_id=:profileId
                    """).setParameter("marker", marker).setParameter("now", completedAt)
                    .setParameter("profileId", profileId).executeUpdate();
        }
        entityManager.createNativeQuery("""
                update iam.account_identities set provider_subject=:marker, updated_at=:now
                 where account_id=:accountId
                """).setParameter("marker", marker).setParameter("now", completedAt)
                .setParameter("accountId", accountId).executeUpdate();
        entityManager.createNativeQuery("""
                update operations.chat_conversation_members
                   set status='REMOVED', left_at=:now, updated_at=:now
                 where account_id=:accountId and status='ACTIVE'
                """).setParameter("now", completedAt).setParameter("accountId", accountId).executeUpdate();
        entityManager.createNativeQuery("""
                insert into iam.account_status_history(account_status_history_id, account_id, old_status,
                                                       new_status, reason, changed_at)
                values (gen_random_uuid(), :accountId, :oldStatus, 'DELETED',
                        'Customer account deletion grace period completed', :now)
                """).setParameter("accountId", accountId).setParameter("oldStatus", account.getStatus().name())
                .setParameter("now", completedAt).executeUpdate();

        account.setUsername(marker);
        account.setEmail(AccountDeletionTombstones.email(accountId));
        account.setKeycloakUserId(marker);
        account.setStatus(AccountStatus.DELETED);
        account.setLastLoginAt(null);
        account.setLockedUntil(null);
        account.setFailedLoginCount(0);
        entityManager.flush();
        markCompleted(requestId, completedAt);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID requestId, Instant now, Instant nextRetryAt, int maxAttempts, String code, String message) {
        AccountDeletionRequestEntity entity = repository.findById(requestId).orElseThrow();
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
        repository.saveAndFlush(entity);
    }

    private UUID findCustomerId(UUID accountId) {
        List<?> result = entityManager.createNativeQuery("""
                select customer.customer_id from iam.accounts account
                join people.customers customer on customer.user_profile_id=account.user_profile_id
                where account.account_id=:accountId and customer.status <> 'DELETED'
                """).setParameter("accountId", accountId).getResultList();
        return result.isEmpty() ? null : (UUID) result.getFirst();
    }

    private void addBlocker(List<String> blockers, String code, String sql, UUID customerId) {
        Boolean exists = (Boolean) entityManager.createNativeQuery(sql).setParameter("id", customerId).getSingleResult();
        if (Boolean.TRUE.equals(exists)) blockers.add(code);
    }

    private void markCompleted(UUID requestId, Instant completedAt) {
        AccountDeletionRequestEntity request = repository.findById(requestId).orElseThrow();
        request.setStatus(AccountDeletionRequestStatus.COMPLETED);
        request.setCompletedAt(completedAt);
        request.setClaimToken(null);
        request.setProcessingLeaseUntil(null);
        request.setNextRetryAt(null);
        request.setLastErrorCode(null);
        request.setLastErrorMessage(null);
        repository.saveAndFlush(request);
    }

    private AccountDeletionRequest toDomain(AccountDeletionRequestEntity entity) {
        return new AccountDeletionRequest(entity.getRequestId(), entity.getAccountId(), entity.getStatus(),
                entity.getRequestedAt(), entity.getScheduledDeletionAt(), entity.getCancelledAt(),
                entity.getCompletedAt(), entity.getAttemptCount(), entity.getLastErrorCode());
    }
}
