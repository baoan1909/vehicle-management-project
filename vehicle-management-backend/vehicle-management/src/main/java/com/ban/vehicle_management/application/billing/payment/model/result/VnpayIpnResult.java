package com.ban.vehicle_management.application.billing.payment.model.result;

public record VnpayIpnResult(
        String responseCode,
        String message
) {
}
