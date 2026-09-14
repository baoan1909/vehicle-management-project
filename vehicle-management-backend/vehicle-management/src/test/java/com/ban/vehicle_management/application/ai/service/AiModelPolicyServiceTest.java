package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiDataMode;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import org.junit.jupiter.api.Test;

class AiModelPolicyServiceTest {

    @Test
    void unpaidModeRequiresFreeTierApproval() {
        AiAssistantProperties properties = new AiAssistantProperties();
        properties.setDataMode(AiDataMode.UNPAID);
        AiModelPolicyService service = new AiModelPolicyService(properties);
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setStatus(AiModelStatus.ACTIVE);
        configuration.setFreeTierApproved(false);

        assertFalse(service.canUse(configuration));

        configuration.setFreeTierApproved(true);
        assertTrue(service.canUse(configuration));

        configuration.setRequiresFunctionCalling(true);
        assertTrue(service.canUse(configuration));
    }

    @Test
    void shouldFallbackOnlyForExpectedFailures() {
        AiModelPolicyService service = new AiModelPolicyService(new AiAssistantProperties());

        assertTrue(service.canFallbackFor("HTTP_429"));
        assertTrue(service.canFallbackFor("MODEL_NOT_FOUND"));
        assertFalse(service.canFallbackFor("HTTP_403"));
        assertFalse(service.canFallbackFor("HTTP_400"));
    }
}
