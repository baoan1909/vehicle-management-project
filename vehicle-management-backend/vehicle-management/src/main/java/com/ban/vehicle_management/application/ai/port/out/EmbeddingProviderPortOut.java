package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;

public interface EmbeddingProviderPortOut {

    AiProvider provider();

    EmbeddingResult embed(EmbeddingRequest request, AiModelConfiguration configuration);
}