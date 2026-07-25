package com.ban.vehicle_management.entrypoint.dto.billing.payment.request;

public record CreateVnpayPaymentRequest(
        String bankCode,
        String locale
) {
}
