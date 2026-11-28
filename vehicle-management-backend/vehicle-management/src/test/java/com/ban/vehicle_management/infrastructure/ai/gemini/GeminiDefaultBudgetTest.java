package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.*;
import com.ban.vehicle_management.application.ai.service.AiAssistantProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class GeminiDefaultBudgetTest {
    @Test void defaultGenerationCanAccommodateTheObservedSixteenSecondGoogleResponseWithinAFinitePlan() {
        AiAssistantProperties assistant = new AiAssistantProperties();
        GeminiProperties provider = new GeminiProperties();
        assertTrue(assistant.getGenerationTimeout().compareTo(Duration.ofSeconds(16)) > 0);
        assertTrue(provider.getCallBudget().compareTo(assistant.getGenerationTimeout()) > 0);
        assertTrue(provider.getCallBudget().compareTo(Duration.ofMinutes(1)) <= 0);
    }
    @Test void anExplicitShorterRequestTimeoutStillCapsGeneration() {
        AiAssistantProperties assistant = new AiAssistantProperties();
        assistant.setRequestTimeout(Duration.ofSeconds(2));
        assertEquals(Duration.ofSeconds(2), assistant.getGenerationTimeout());
        assistant.setGenerationTimeout(Duration.ofMillis(500));
        assertEquals(Duration.ofMillis(500), assistant.getGenerationTimeout());
    }
}
