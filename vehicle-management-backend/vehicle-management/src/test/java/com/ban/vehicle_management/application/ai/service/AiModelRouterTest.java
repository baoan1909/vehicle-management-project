package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiModelRouterTest {

    @Mock
    private AiModelConfigurationPortOut modelConfigurationPortOut;

    @Test
    void activeModelIsSelectedBeforeFallbackWithoutHardcodedModelId() {
        AiAssistantProperties properties = new AiAssistantProperties();
        AiModelRouter router = new AiModelRouter(
                modelConfigurationPortOut,
                new AiModelPolicyService(properties)
        );
        AiModelConfiguration fallback = config("model-fallback", AiModelStatus.FALLBACK, 1);
        AiModelConfiguration active = config("model-active-from-db", AiModelStatus.ACTIVE, 99);
        when(modelConfigurationPortOut.findEnabledByUseCase(AiUseCase.SUPPORT_CHAT)).thenReturn(List.of(fallback, active));

        List<AiModelConfiguration> candidates = router.resolveCandidates(AiUseCase.SUPPORT_CHAT);

        assertEquals("model-active-from-db", candidates.getFirst().getModelId());
    }

    private AiModelConfiguration config(String modelId, AiModelStatus status, int priority) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setModelId(modelId);
        configuration.setStatus(status);
        configuration.setPriority(priority);
        configuration.setFreeTierApproved(true);
        return configuration;
    }
}
