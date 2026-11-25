package com.ban.vehicle_management.application.ai.service;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
class AiModelFallbackPolicyTest {
    private final AiModelPolicyService policy=new AiModelPolicyService(new AiAssistantProperties());
    @ParameterizedTest @ValueSource(strings={"HTTP_5XX","HTTP_429","HTTP_408","TIMEOUT","PROVIDER_ERROR","PROVIDER_EXCEPTION","MODEL_NOT_FOUND"})
    void recoverableFailuresAllowAnotherConfiguredModel(String code) { assertTrue(policy.canFallbackFor(code)); }
    @ParameterizedTest @ValueSource(strings={"HTTP_400","HTTP_401","HTTP_403","SAFETY_BLOCKED","GEMINI_API_KEY_MISSING"})
    void clientOrSafetyFailuresDoNotAllowAnotherModel(String code) { assertFalse(policy.canFallbackFor(code)); }
}
