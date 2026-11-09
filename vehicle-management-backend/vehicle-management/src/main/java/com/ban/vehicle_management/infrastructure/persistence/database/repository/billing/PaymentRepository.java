package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {

    @Query(value = """
        select payment.payment_id from billing.payments payment
        join billing.invoices invoice on invoice.invoice_id=payment.invoice_id
        where invoice.customer_id=:customerId and payment.status='PENDING' limit 1
        """, nativeQuery = true)
    Optional<UUID> findDeletionBlockingPaymentId(@Param("customerId") UUID customerId);

    List<PaymentEntity> findByInvoiceIdOrderByPaidAtAsc(UUID invoiceId);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    boolean existsByTransactionRefAndStatus(String transactionRef, PaymentStatus status);

    Optional<PaymentEntity> findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(
            UUID invoiceId,
            PaymentStatus status
    );

    Optional<PaymentEntity> findByTransactionRef(String transactionRef);

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.paymentId = :paymentId")
    Optional<PaymentEntity> findByIdForUpdate(@Param("paymentId") UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from PaymentEntity payment
            where payment.transactionRef = :transactionRef
            """)
    Optional<PaymentEntity> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);

    @Query("""
            select payment from PaymentEntity payment
            join payment.invoice invoice
            where payment.status = 'SUCCESS'
              and invoice.customerId = :customerId
              and invoice.parkingLotId = :parkingLotId
              and (invoice.parkingSessionId is not null or invoice.subscriptionId is not null)
            order by coalesce(payment.paidAt, payment.createdAt) desc, payment.paymentId desc
            """)
    List<PaymentEntity> findLatestReviewEligiblePayment(
            @Param("customerId") UUID customerId,
            @Param("parkingLotId") UUID parkingLotId,
            Pageable pageable);
}
