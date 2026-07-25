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

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {

    List<PaymentEntity> findByInvoiceIdOrderByPaidAtAsc(UUID invoiceId);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    boolean existsByTransactionRefAndStatus(String transactionRef, PaymentStatus status);

    Optional<PaymentEntity> findFirstByInvoiceIdAndStatusOrderByCreatedAtDesc(
            UUID invoiceId,
            PaymentStatus status
    );

    Optional<PaymentEntity> findByTransactionRef(String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from PaymentEntity payment
            where payment.transactionRef = :transactionRef
            """)
    Optional<PaymentEntity> findByTransactionRefForUpdate(@Param("transactionRef") String transactionRef);
}
