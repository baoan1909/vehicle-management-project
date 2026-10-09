package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.wallet.port.out.WalletTopupPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.WalletTopupOrder;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.WalletTopupOrderRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WalletTopupPersistenceAdapter implements WalletTopupPortOut {

    private final WalletTopupOrderRepository repository;
    private final WalletPersistenceMapper mapper;
    private final EntityManager entityManager;

    public WalletTopupPersistenceAdapter(
            WalletTopupOrderRepository repository,
            WalletPersistenceMapper mapper,
            EntityManager entityManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Override
    public void lockIdempotencyKey(String idempotencyKey) {
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(CAST(:idempotencyKey AS text), 0))")
                .setParameter("idempotencyKey", idempotencyKey)
                .getSingleResult();
    }

    @Override
    public WalletTopupOrder save(WalletTopupOrder order) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(order)));
    }

    @Override
    public Optional<WalletTopupOrder> findById(UUID topupOrderId) {
        return repository.findById(topupOrderId).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTopupOrder> findByIdForUpdate(UUID topupOrderId) {
        return repository.findByIdForUpdate(topupOrderId).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTopupOrder> findByTransactionRef(String transactionRef) {
        return repository.findByTransactionRef(transactionRef).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTopupOrder> findByTransactionRefForUpdate(String transactionRef) {
        return repository.findByTransactionRefForUpdate(transactionRef).map(mapper::toDomain);
    }

    @Override
    public Optional<WalletTopupOrder> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public List<WalletTopupOrder> findByWallet(UUID walletId, int page, int size) {
        return repository.findByWalletIdOrderByCreatedAtDesc(walletId, PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public List<UUID> claimExpiredPending(int batchSize, Instant now) {
        return repository.claimExpiredPending(now, batchSize);
    }

    @Override
    public List<WalletTopupOrder> findReconciliationExceptions(int page, int size) {
        return repository.findPendingReconciliation(Instant.now(), PageRequest.of(page, size)).stream()
                .map(mapper::toDomain).toList();
    }
}
