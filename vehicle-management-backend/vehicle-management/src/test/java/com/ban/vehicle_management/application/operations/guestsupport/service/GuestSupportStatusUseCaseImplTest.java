package com.ban.vehicle_management.application.operations.guestsupport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.CaptchaPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestSupportStatusUseCaseImplTest {

    @Mock
    private GuestPublicAssistantService assistantService;
    @Mock
    private CaptchaProofService captchaProofService;

    private GuestSupportProperties properties;
    private GuestSupportStatusUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        properties = new GuestSupportProperties();
        useCase = new GuestSupportStatusUseCaseImpl(
                assistantService, captchaProofService, properties);
    }

    @Test
    void getStatusHidesProviderConfigurationWhenCaptchaIsDisabled() {
        when(assistantService.isEnabled()).thenReturn(true);

        var result = useCase.getStatus();

        assertTrue(result.enabled());
        assertFalse(result.captchaEnabled());
        assertNull(result.captchaProvider());
        assertNull(result.captchaSiteKey());
    }

    @Test
    void getStatusReturnsEnabledCaptchaConfigurationAndPurposeRequirements() {
        properties.setCaptchaEnabled(true);
        properties.setCaptchaProvider("TURNSTILE");
        properties.setCaptchaSiteKey("site-key");
        when(captchaProofService.isRequired(CaptchaPurpose.INTAKE)).thenReturn(true);
        when(captchaProofService.isRequired(CaptchaPurpose.ASSISTANT)).thenReturn(false);

        var result = useCase.getStatus();

        assertTrue(result.captchaEnabled());
        assertEquals("TURNSTILE", result.captchaProvider());
        assertEquals("site-key", result.captchaSiteKey());
        assertTrue(result.captchaRequiredForIntake());
        assertFalse(result.captchaRequiredForAssistant());
    }
}
