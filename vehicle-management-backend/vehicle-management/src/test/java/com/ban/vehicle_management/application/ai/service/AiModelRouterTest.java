package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;
import java.util.UUID;
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

    @Test
    void rolloutSelectionIsStableForSameConversation() {
        AiAssistantProperties properties = new AiAssistantProperties();
        AiModelRouter router = new AiModelRouter(
                modelConfigurationPortOut,
                new AiModelPolicyService(properties)
        );
        AiModelConfiguration first = config("model-a", AiModelStatus.ACTIVE, 1);
        first.setRolloutPercentage(40);
        AiModelConfiguration second = config("model-b", AiModelStatus.ACTIVE, 2);
        second.setRolloutPercentage(60);
        AiModelConfiguration fallback = config("model-fallback", AiModelStatus.FALLBACK, 3);
        when(modelConfigurationPortOut.findEnabledByUseCase(AiUseCase.SUPPORT_CHAT))
                .thenReturn(List.of(first, second, fallback));

        List<AiModelConfiguration> firstResolution = router.resolveCandidates(
                AiUseCase.SUPPORT_CHAT,
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                1
        );
        List<AiModelConfiguration> secondResolution = router.resolveCandidates(
                AiUseCase.SUPPORT_CHAT,
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                1
        );

        assertEquals(firstResolution.getFirst().getModelId(), secondResolution.getFirst().getModelId());
        assertEquals("model-fallback", firstResolution.get(1).getModelId());
    }

    @Test
    void activeRolloutMustSumToOneHundred() {
        AiAssistantProperties properties = new AiAssistantProperties();
        AiModelRouter router = new AiModelRouter(
                modelConfigurationPortOut,
                new AiModelPolicyService(properties)
        );
        AiModelConfiguration active = config("model-a", AiModelStatus.ACTIVE, 1);
        active.setRolloutPercentage(90);
        when(modelConfigurationPortOut.findEnabledByUseCase(AiUseCase.SUPPORT_CHAT)).thenReturn(List.of(active));

        assertThrows(
                com.ban.vehicle_management.shared.exception.BadRequestException.class,
                () -> router.resolveCandidates(
                        AiUseCase.SUPPORT_CHAT,
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        UUID.fromString("20000000-0000-0000-0000-000000000001"),
                        1
                )
        );
    }

    private AiModelConfiguration config(String modelId, AiModelStatus status, int priority) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setModelId(modelId);
        configuration.setStatus(status);
        configuration.setPriority(priority);
        configuration.setRolloutPercentage(status == AiModelStatus.ACTIVE ? 100 : 0);
        configuration.setFreeTierApproved(true);
        return configuration;
    }
}
