package com.ban.vehicle_management.application.billing.payment.port.out;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentPortOut {

    Payment save(Payment payment);

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findAll(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword
    );

    boolean existsByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    boolean existsByTransactionRefAndStatus(String transactionRef, PaymentStatus status);

    Optional<Payment> findFirstByInvoiceIdAndStatus(UUID invoiceId, PaymentStatus status);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Optional<Payment> findByTransactionRefForUpdate(String transactionRef);
}
