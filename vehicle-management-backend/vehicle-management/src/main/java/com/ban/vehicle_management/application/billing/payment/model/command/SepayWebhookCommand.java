package com.ban.vehicle_management.application.billing.payment.model.command;

public record SepayWebhookCommand(
        byte[] rawBody,
        String signature,
        String timestamp
) { }
