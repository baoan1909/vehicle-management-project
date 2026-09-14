package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AiModelAdminPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiModelCatalogPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiModelWarningPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningSeverity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelAdminUseCaseImpl implements AiModelAdminPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final AiModelCatalogPortOut catalogPortOut;
    private final AiModelWarningPortOut warningPortOut;
    private final List<AiProviderPortOut> providerPorts;

    public AiModelAdminUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            AiModelConfigurationPortOut configurationPortOut,
            AiModelCatalogPortOut catalogPortOut,
            AiModelWarningPortOut warningPortOut,
            List<AiProviderPortOut> providerPorts
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.configurationPortOut = configurationPortOut;
        this.catalogPortOut = catalogPortOut;
        this.warningPortOut = warningPortOut;
        this.providerPorts = providerPorts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModelConfiguration> listConfigurations() {
        currentAccountPortIn.requirePermission("AI_MODEL_READ_ALL");
        return configurationPortOut.findAll();
    }

    @Override
    @Transactional
    public AiModelConfiguration updateStatus(UUID configurationId, AiModelStatus status) {
        currentAccountPortIn.requirePermission("AI_MODEL_MANAGE_ALL");
        AiModelConfiguration configuration = findConfiguration(configurationId);
        configuration.setStatus(status);
        if (status == AiModelStatus.ACTIVE) {
            validateActivation(configuration);
        }
        AiModelConfiguration saved = configurationPortOut.save(configuration);
        reconcileRollout(saved);
        return saved;
    }

    @Override
    @Transactional
    public AiModelConfiguration updateRollout(UUID configurationId, int rolloutPercentage) {
        currentAccountPortIn.requirePermission("AI_MODEL_MANAGE_ALL");
        if (rolloutPercentage < 0 || rolloutPercentage > 100) {
            throw new BadRequestException("rolloutPercentage must be between 0 and 100");
        }
        AiModelConfiguration configuration = findConfiguration(configurationId);
        configuration.setRolloutPercentage(rolloutPercentage);
        AiModelConfiguration saved = configurationPortOut.save(configuration);
        reconcileRollout(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiProviderModel> listCatalog() {
        currentAccountPortIn.requirePermission("AI_MODEL_READ_ALL");
        return catalogPortOut.findAll();
    }

    @Override
    @Transactional
    public List<AiProviderModel> syncCatalog() {
        currentAccountPortIn.requirePermission("AI_CATALOG_SYNC_ALL");
        AiProviderPortOut gemini = providerPorts.stream()
                .filter(port -> port.provider() == AiProvider.GEMINI)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Gemini provider is not configured"));
        List<AiProviderModel> models = gemini.listModels();
        if (models.isEmpty()) {
            openWarning(AiProvider.GEMINI, null, null, "CATALOG_SYNC_FAILED", AiModelWarningSeverity.CRITICAL, "Gemini catalog sync returned no models");
        }
        catalogPortOut.upsertGeminiModels(models);
        reconcileConfigurations();
        return catalogPortOut.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModelWarning> listWarnings() {
        currentAccountPortIn.requirePermission("AI_MODEL_READ_ALL");
        return warningPortOut.findByStatuses(List.of(AiModelWarningStatus.OPEN, AiModelWarningStatus.ACKNOWLEDGED));
    }

    private AiModelConfiguration findConfiguration(UUID configurationId) {
        return configurationPortOut.findById(configurationId)
                .orElseThrow(() -> new NotFoundException("AI model configuration not found"));
    }

    private void validateActivation(AiModelConfiguration configuration) {
        boolean inCatalog = catalogPortOut.findAll().stream()
                .anyMatch(model -> model.provider() == configuration.getProvider()
                        && model.modelId().equals(configuration.getModelId()));
        if (!inCatalog) {
            openWarning(configuration.getProvider(), configuration.getModelId(), configuration.getConfigurationId(),
                    "MODEL_NOT_FOUND", AiModelWarningSeverity.CRITICAL, "Model is not present in catalog");
            throw new BadRequestException("Cannot activate model that is not present in catalog");
        }
    }

    private void reconcileRollout(AiModelConfiguration changed) {
        List<AiModelConfiguration> active = configurationPortOut.findByUseCaseAndStatus(changed.getUseCase(), AiModelStatus.ACTIVE);
        int total = active.stream().map(AiModelConfiguration::getRolloutPercentage).mapToInt(value -> value == null ? 0 : value).sum();
        if (total != 100) {
            openWarning(changed.getProvider(), changed.getModelId(), changed.getConfigurationId(),
                    "CAPABILITY_MISMATCH", AiModelWarningSeverity.WARNING, "ACTIVE rollout percentage total is " + total);
            throw new BadRequestException("ACTIVE rollout percentage must sum to 100");
        }
    }

    private void reconcileConfigurations() {
        List<AiProviderModel> catalog = catalogPortOut.findAll();
        for (AiModelConfiguration configuration : configurationPortOut.findAll()) {
            AiProviderModel model = catalog.stream()
                    .filter(item -> item.provider() == configuration.getProvider() && item.modelId().equals(configuration.getModelId()))
                    .findFirst()
                    .orElse(null);
            if (model == null) {
                openWarning(configuration.getProvider(), configuration.getModelId(), configuration.getConfigurationId(),
                        "MODEL_NOT_FOUND", AiModelWarningSeverity.CRITICAL, "Configured model was not found in latest catalog");
                continue;
            }
            if (!model.supportedActions().contains("generateContent") && configuration.getUseCase() != com.ban.vehicle_management.shared.enumeration.ai.AiUseCase.EMBEDDING) {
                openWarning(configuration.getProvider(), configuration.getModelId(), configuration.getConfigurationId(),
                        "CAPABILITY_MISMATCH", AiModelWarningSeverity.CRITICAL, "Configured model does not support generateContent");
            }
            if (configuration.getStatus() == AiModelStatus.ACTIVE && !model.supportedActions().contains("generateContent")) {
                openWarning(configuration.getProvider(), configuration.getModelId(), configuration.getConfigurationId(),
                        "ACTIVE_MODEL_UNAVAILABLE", AiModelWarningSeverity.CRITICAL, "ACTIVE model is unavailable for generation");
            }
        }
    }

    private void openWarning(
            AiProvider provider,
            String modelId,
            UUID configurationId,
            String warningCode,
            AiModelWarningSeverity severity,
            String detail
    ) {
        AiModelWarning warning = warningPortOut.findOpen(provider.name(), modelId, configurationId, warningCode)
                .orElseGet(AiModelWarning::new);
        if (warning.getWarningId() == null) {
            warning.setWarningId(UUID.randomUUID());
            warning.setCreatedAt(Instant.now());
        }
        warning.setProvider(provider);
        warning.setModelId(modelId);
        warning.setConfigurationId(configurationId);
        warning.setWarningCode(warningCode);
        warning.setSeverity(severity);
        warning.setStatus(AiModelWarningStatus.OPEN);
        warning.setDetail(detail);
        warning.setDetectedAt(Instant.now());
        warningPortOut.save(warning);
    }
}
