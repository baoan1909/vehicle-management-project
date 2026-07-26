package com.ban.vehicle_management.application.billing.payment.model.command;

import java.util.Map;

public record VnpayCallbackCommand(
        Map<String, String> parameters
) {
}
