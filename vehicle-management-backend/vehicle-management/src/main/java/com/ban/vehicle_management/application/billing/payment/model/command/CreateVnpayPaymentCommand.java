package com.ban.vehicle_management.application.billing.payment.model.command;

public record CreateVnpayPaymentCommand(
        String bankCode,
        String locale,
        String clientIp
) {
}
