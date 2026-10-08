package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.infrastructure.mapper.billing.PaymentPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.PaymentRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.specification.billing.PaymentSpecifications;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceAdapter implements PaymentPortOut {

    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    public PaymentPersistenceAdapter(
            PaymentRepository paymentRepository,
            PaymentPersistenceMapper paymentPersistenceMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentPersistenceMapper = paymentPersistenceMapper;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity savedEntity = paymentRepository.saveAndFlush(paymentPersistenceMapper.toEntity(payment));
        return paymentPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public List<Payment> findByInvoiceId(UUID invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaidAtAsc(invoiceId)
                .stream()
                .map(paymentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findAll(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        return findAll(invoiceId, paymentMethod, status, receivedBy, fromDate, toDate, keyword, null);
    }

    @Override
    public List<Payment> findAll(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword,
            Set<UUID> parkingLotIds
    ) {
        return paymentRepository.findAll(PaymentSpecifications.withFilters(
                        invoiceId,
                        paymentMethod,
                        status,
                        receivedBy,
                        fromDate,
                        toDate,
                        keyword,
                        parkingLotIds
                )).stream()
                .map(paymentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status) {
        return paymentRepository.existsByInvoiceIdAndStatus(invoiceId, status);
    }

    @Override
    public boolean existsByTransactionRefAndStatus(String transactionRef, PaymentStatus status) {
        return paymentRepository.existsByTransactionRefAndStatus(transactionRef, status);
    }

    @Override
    public Optional<Payment> findFirstByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status) {
        return paymentRepository.findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(invoiceId, status)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByTransactionRef(String transactionRef) {
        return paymentRepository.findByTransactionRef(transactionRef)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByTransactionRefForUpdate(String transactionRef) {
        return paymentRepository.findByTransactionRefForUpdate(transactionRef)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(paymentPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(UUID paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .map(paymentPersistenceMapper::toDomain);
    }
}
