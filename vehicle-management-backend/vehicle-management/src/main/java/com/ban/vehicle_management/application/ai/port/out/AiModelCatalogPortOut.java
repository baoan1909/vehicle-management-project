package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import java.util.List;

public interface AiModelCatalogPortOut {

    void upsertGeminiModels(List<AiProviderModel> models);

    List<AiProviderModel> findAll();
}
