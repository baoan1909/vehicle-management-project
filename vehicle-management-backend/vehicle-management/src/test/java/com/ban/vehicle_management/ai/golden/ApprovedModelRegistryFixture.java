package com.ban.vehicle_management.ai.golden;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Test-only import of model metadata already approved by the application registry.
 * Never changes the developer database or turns free-tier approval on. */
final class ApprovedModelRegistryFixture {
    private ApprovedModelRegistryFixture() { }
    static List<AiModelConfiguration> load(Path path, ObjectMapper mapper) throws java.io.IOException {
        var root = mapper.readTree(path.toFile());
        if (!root.path("models").isArray()) throw new IllegalArgumentException("Model registry snapshot requires models array");
        List<AiModelConfiguration> models = new ArrayList<>();
        var seen = new java.util.HashSet<String>();
        for (var row : root.path("models")) {
            if (!row.path("freeTierApproved").isBoolean() || !row.path("freeTierApproved").asBoolean()) continue;
            String status = row.path("status").asText();
            if (!List.of("ACTIVE", "FALLBACK").contains(status)) continue;
            String id = row.path("modelId").asText();
            if (!id.matches("[a-z0-9][a-z0-9._-]{0,120}") || !seen.add(id))
                throw new IllegalArgumentException("Invalid or duplicate model ID in registry snapshot");
            var model = mapper.treeToValue(row, AiModelConfiguration.class);
            model.setConfigurationId(UUID.randomUUID()); model.setProvider(AiProvider.GEMINI);
            model.setUseCase(AiUseCase.SUPPORT_CHAT); model.setStatus(AiModelStatus.valueOf(status));
            models.add(model);
        }
        if (models.isEmpty()) throw new IllegalArgumentException("No enabled approved-free models in snapshot");
        return List.copyOf(models);
    }
}
