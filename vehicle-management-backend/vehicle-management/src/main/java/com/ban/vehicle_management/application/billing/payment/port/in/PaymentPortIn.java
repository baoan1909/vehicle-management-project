package com.ban.vehicle_management.application.billing.payment.port.in;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentPortIn {

    Payment recordPayment(UUID invoiceId, Payment payment);

    List<Payment> getPayments(
            UUID invoiceId,
            PaymentMethod paymentMethod,
            PaymentStatus status,
            UUID receivedBy,
            Instant fromDate,
            Instant toDate,
            String keyword
    );
}