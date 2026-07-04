package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.PaymentEntity;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID>, JpaSpecificationExecutor<PaymentEntity> {

    List<PaymentEntity> findByInvoiceIdOrderByPaidAtAsc(UUID invoiceId);

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    boolean existsByTransactionRefAndStatus(String transactionRef, PaymentStatus status);
}