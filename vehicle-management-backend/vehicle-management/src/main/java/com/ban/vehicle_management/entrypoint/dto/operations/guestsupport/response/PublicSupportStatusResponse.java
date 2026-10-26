package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response;

public record PublicSupportStatusResponse(
        boolean enabled,
        boolean captchaEnabled,
        String captchaProvider,
        String captchaSiteKey,
        boolean captchaRequiredForIntake,
        boolean captchaRequiredForAssistant
) {
}
