package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiModelRouter {

    private final AiModelConfigurationPortOut modelConfigurationPortOut;
    private final AiModelPolicyService policyService;

    public AiModelRouter(
            AiModelConfigurationPortOut modelConfigurationPortOut,
            AiModelPolicyService policyService
    ) {
        this.modelConfigurationPortOut = modelConfigurationPortOut;
        this.policyService = policyService;
    }

    public List<AiModelConfiguration> resolveCandidates(AiUseCase useCase) {
        List<AiModelConfiguration> candidates = modelConfigurationPortOut.findEnabledByUseCase(useCase).stream()
                .filter(policyService::canUse)
                .sorted(Comparator
                        .comparing((AiModelConfiguration config) -> config.getStatus() == AiModelStatus.ACTIVE ? 0 : 1)
                        .thenComparing(config -> config.getPriority() == null ? Integer.MAX_VALUE : config.getPriority()))
                .toList();
        if (candidates.isEmpty()) {
            throw new BadRequestException("No AI model configuration is enabled for " + useCase);
        }
        return candidates;
    }

    public List<AiModelConfiguration> resolveCandidates(AiUseCase useCase, UUID conversationId, UUID accountId, int rolloutVersion) {
        List<AiModelConfiguration> enabled = resolveCandidates(useCase);
        List<AiModelConfiguration> active = enabled.stream()
                .filter(configuration -> configuration.getStatus() == AiModelStatus.ACTIVE)
                .sorted(Comparator
                        .comparing((AiModelConfiguration config) -> config.getPriority() == null ? Integer.MAX_VALUE : config.getPriority())
                        .thenComparing(config -> config.getCreatedAt() == null ? java.time.Instant.EPOCH : config.getCreatedAt()))
                .toList();
        validateActiveRollout(useCase, active);
        AiModelConfiguration selected = selectActive(active, stableKey(conversationId, accountId), useCase, rolloutVersion);
        List<AiModelConfiguration> ordered = new ArrayList<>();
        ordered.add(selected);
        enabled.stream()
                .filter(configuration -> configuration.getStatus() == AiModelStatus.FALLBACK)
                .sorted(Comparator.comparing(config -> config.getPriority() == null ? Integer.MAX_VALUE : config.getPriority()))
                .forEach(ordered::add);
        return ordered;
    }

    private void validateActiveRollout(AiUseCase useCase, List<AiModelConfiguration> active) {
        if (active.isEmpty()) {
            throw new BadRequestException("No ACTIVE AI model configuration is enabled for " + useCase);
        }
        int total = active.stream()
                .map(AiModelConfiguration::getRolloutPercentage)
                .mapToInt(value -> value == null ? 0 : value)
                .sum();
        if (total != 100) {
            throw new BadRequestException("ACTIVE AI rollout percentage must sum to 100 for " + useCase);
        }
    }

    private AiModelConfiguration selectActive(List<AiModelConfiguration> active, String stableKey, AiUseCase useCase, int rolloutVersion) {
        if (active.size() == 1) {
            AiModelConfiguration only = active.getFirst();
            if (only.getRolloutPercentage() == null || only.getRolloutPercentage() != 100) {
                throw new BadRequestException("Single ACTIVE AI model must have rollout percentage 100");
            }
            return only;
        }
        int bucket = Math.floorMod(hash(stableKey + ":" + useCase + ":" + rolloutVersion), 100);
        int upperBound = 0;
        for (AiModelConfiguration configuration : active) {
            upperBound += configuration.getRolloutPercentage() == null ? 0 : configuration.getRolloutPercentage();
            if (bucket < upperBound) {
                return configuration;
            }
        }
        return active.getLast();
    }

    private String stableKey(UUID conversationId, UUID accountId) {
        if (conversationId != null) {
            return conversationId.toString();
        }
        if (accountId != null) {
            return accountId.toString();
        }
        return "anonymous";
    }

    private int hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return ((digest[0] & 0xff) << 24)
                    | ((digest[1] & 0xff) << 16)
                    | ((digest[2] & 0xff) << 8)
                    | (digest[3] & 0xff);
        } catch (NoSuchAlgorithmException exception) {
            return value.hashCode();
        }
    }
}
