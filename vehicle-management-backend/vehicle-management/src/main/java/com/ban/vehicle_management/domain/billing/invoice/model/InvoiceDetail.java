package com.ban.vehicle_management.domain.billing.invoice.model;

import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceDetail {

    private Invoice invoice;
    private List<Payment> payments;
}