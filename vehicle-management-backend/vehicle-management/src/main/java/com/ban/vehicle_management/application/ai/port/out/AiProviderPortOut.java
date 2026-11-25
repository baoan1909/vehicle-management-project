package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiProviderAttempt;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.List;

public interface AiProviderPortOut {

    AiProvider provider();

    AiResponse generate(AiRequest request, AiModelConfiguration configuration);

    default List<AiProviderAttempt> generationAttempts(List<AiModelConfiguration> configurations) {
        return configurations.stream().map(model -> new AiProviderAttempt(model, null, null)).toList();
    }

    default AiResponse generateAttempt(AiRequest request, AiProviderAttempt attempt) {
        return generate(request, attempt.configuration());
    }

    default boolean managesAvailability() { return false; }

    List<AiProviderModel> listModels();
}
