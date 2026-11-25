package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual registration of the two explicitly configured local models, never auto-activation. */
@Service
public class LocalAiModelRegistrationService {
    private final AiModelConfigurationPortOut configurations;
    private final CurrentAccountPortIn accounts;
    public LocalAiModelRegistrationService(AiModelConfigurationPortOut configurations, CurrentAccountPortIn accounts) {
        this.configurations = configurations;
        this.accounts = accounts;
    }
    @Transactional
    public void registerCandidates(List<AiProviderModel> configuredModels) {
        if (configuredModels.stream().noneMatch(model -> model.provider() == AiProvider.LM_STUDIO)) return;
        accounts.requirePermission("AI_MODEL_MANAGE_ALL");
        var actor = accounts.getCurrentAccountOrThrow();
        if (!"system_admin".equalsIgnoreCase(actor.roleCode())) throw new AccessDeniedException("Chỉ quản trị hệ thống được đăng ký model local");
        var existing = configurations.findAll();
        for (var model : configuredModels) {
            if (model.provider() != AiProvider.LM_STUDIO) continue;
            AiUseCase useCase = model.supportedActions().contains("embedContent") ? AiUseCase.EMBEDDING : AiUseCase.SUPPORT_CHAT;
            if (existing.stream().anyMatch(config -> config.getProvider() == AiProvider.LM_STUDIO
                    && config.getUseCase() == useCase && config.getModelId().equals(model.modelId()))) continue;
            var config = new AiModelConfiguration();
            config.setConfigurationId(UUID.nameUUIDFromBytes(("LM_STUDIO:" + useCase + ":" + model.modelId()).getBytes(StandardCharsets.UTF_8)));
            config.setProvider(AiProvider.LM_STUDIO);config.setUseCase(useCase);config.setModelId(model.modelId());
            config.setTemperature(BigDecimal.valueOf(.3));config.setPriority(100);config.setRolloutPercentage(100);
            config.setStatus(AiModelStatus.CANDIDATE);config.setMaxOutputTokens(useCase == AiUseCase.EMBEDDING ? 1 : 512);
            config.setOutputDimension(useCase == AiUseCase.EMBEDDING ? 768 : null);
            config.setRequiresStructuredOutput(useCase == AiUseCase.SUPPORT_CHAT);config.setRequiresFunctionCalling(false);
            // Zero cloud-token cost for these configured local models; activation remains explicit.
            config.setFreeTierApproved(true);config.setCreatedAt(Instant.now());config.setCreatedBy(actor.accountId());
            configurations.save(config);
        }
    }
}
