package com.ban.vehicle_management.application.operations.guestsupport.model;

public record GuestSupportStatusResult(
        boolean enabled,
        boolean captchaEnabled,
        String captchaProvider,
        String captchaSiteKey,
        boolean captchaRequiredForIntake,
        boolean captchaRequiredForAssistant) {
}
