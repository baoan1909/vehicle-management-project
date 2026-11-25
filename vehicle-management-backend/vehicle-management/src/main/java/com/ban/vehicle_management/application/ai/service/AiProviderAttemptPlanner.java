package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.*;

/** Preserves registry-approved models and asks each provider for its credential order. */
public final class AiProviderAttemptPlanner {
    private AiProviderAttemptPlanner() { }

    public static List<AiProviderAttempt> plan(List<AiModelConfiguration> configurations,
            Map<AiProvider, AiProviderPortOut> providers) {
        Map<AiProvider, List<AiModelConfiguration>> groups = new LinkedHashMap<>();
        configurations.forEach(configuration -> groups.computeIfAbsent(
                configuration.getProvider() == null ? AiProvider.GEMINI : configuration.getProvider(),
                ignored -> new ArrayList<>()).add(configuration));
        List<AiProviderAttempt> result = new ArrayList<>();
        groups.forEach((providerName, models) -> {
            AiProviderPortOut provider = providers.get(providerName);
            List<AiProviderAttempt> planned = provider == null ? List.of() : provider.generationAttempts(models);
            if (planned == null || planned.isEmpty()) {
                models.forEach(model -> result.add(new AiProviderAttempt(model, null, null)));
            } else result.addAll(planned);
        });
        return List.copyOf(result);
    }

    public static AiResponse generate(AiProviderPortOut provider, AiRequest request, AiProviderAttempt attempt) {
        return attempt.credentialAlias() == null ? provider.generate(request, attempt.configuration())
                : provider.generateAttempt(request, attempt);
    }
}
