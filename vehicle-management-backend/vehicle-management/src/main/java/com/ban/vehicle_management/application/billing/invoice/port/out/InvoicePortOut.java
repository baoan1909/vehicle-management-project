package com.ban.vehicle_management.application.billing.invoice.port.out;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoicePortOut {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID invoiceId);

    List<Invoice> findAll(
            UUID customerId,
            UUID parkingSessionId,
            UUID subcriptionId,
            UUID lostCardReportId,
            InvoiceStatus status,
            Instant fromDate,
            Instant toDate,
            String keywword
    );

    boolean existsCustomerById(UUID customerId);
    boolean existsParkingSessionById(UUID parkingSeeionId);
    boolean existsSubcriptionById(UUID subcriptionId);
    boolean existsLostCardReportById(UUID lostCardReportId);
    boolean existsByParkingSessionIdAndStatusIn(UUID parkingSessionId, Collection<InvoiceStatus> statuses);
    boolean existsBySubscriptionIdAndStatusIn(UUID subscriptionId, Collection<InvoiceStatus> statuses);
    boolean existsByLostCardReportIdAndStatusIn(UUID lostCardReportId, Collection<InvoiceStatus> statuses);

    Optional<Invoice> findFirstBySubscriptionIdAndStatus(
            UUID subscriptionId,
            InvoiceStatus status
    );

    Optional<Invoice> findFirstBySubscriptionIdAndStatusIn(
            UUID subscriptionId,
            Collection<InvoiceStatus> statuses
    );
}
