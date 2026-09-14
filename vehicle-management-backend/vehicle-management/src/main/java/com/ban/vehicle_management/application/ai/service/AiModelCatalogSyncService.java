package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelCatalogPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AiModelCatalogSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiModelCatalogSyncService.class);

    private final AiAssistantProperties properties;
    private final List<AiProviderPortOut> providerPorts;
    private final AiModelCatalogPortOut catalogPortOut;

    public AiModelCatalogSyncService(
            AiAssistantProperties properties,
            List<AiProviderPortOut> providerPorts,
            AiModelCatalogPortOut catalogPortOut
    ) {
        this.properties = properties;
        this.providerPorts = providerPorts;
        this.catalogPortOut = catalogPortOut;
    }

    @Scheduled(fixedDelayString = "${app.ai.model-catalog-sync-fixed-delay-ms:86400000}", initialDelayString = "${app.ai.model-catalog-sync-initial-delay-ms:60000}")
    public void syncModels() {
        if (!properties.isAssistantEnabled()) {
            return;
        }
        AiProviderPortOut providerPortOut = providerPorts.stream()
                .filter(provider -> provider.provider() == AiProvider.GEMINI)
                .findFirst()
                .orElse(null);
        if (providerPortOut == null) {
            return;
        }
        List<AiProviderModel> models = providerPortOut.listModels();
        catalogPortOut.upsertGeminiModels(models);
        LOGGER.info("Synced Gemini model catalog count={}", models.size());
    }
}
