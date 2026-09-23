package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.List;

public interface AiProviderPortOut {

    AiProvider provider();

    AiResponse generate(AiRequest request, AiModelConfiguration configuration);

    List<AiProviderModel> listModels();
}
