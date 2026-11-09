package com.ban.vehicle_management.application.operations.guestsupport.service;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestSupportStatusResult;
import com.ban.vehicle_management.application.operations.guestsupport.port.in.GuestSupportStatusPortIn;
import org.springframework.stereotype.Service;

@Service
public class GuestSupportStatusUseCaseImpl implements GuestSupportStatusPortIn {

    private final GuestPublicAssistantService assistantService;
    private final CaptchaProofService captchaProofService;
    private final GuestSupportProperties properties;

    public GuestSupportStatusUseCaseImpl(
            GuestPublicAssistantService assistantService,
            CaptchaProofService captchaProofService,
            GuestSupportProperties properties) {
        this.assistantService = assistantService;
        this.captchaProofService = captchaProofService;
        this.properties = properties;
    }

    @Override
    public GuestSupportStatusResult getStatus() {
        boolean captchaEnabled = properties.isCaptchaEnabled();
        return new GuestSupportStatusResult(
                assistantService.isEnabled(),
                captchaEnabled,
                captchaEnabled ? properties.getCaptchaProvider() : null,
                captchaEnabled ? properties.getCaptchaSiteKey() : null,
                captchaProofService.isRequired(CaptchaPurpose.INTAKE),
                captchaProofService.isRequired(CaptchaPurpose.ASSISTANT));
    }
}
