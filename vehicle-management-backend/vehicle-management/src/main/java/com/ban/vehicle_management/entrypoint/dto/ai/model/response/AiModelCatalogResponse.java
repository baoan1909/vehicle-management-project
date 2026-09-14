package com.ban.vehicle_management.entrypoint.dto.ai.model.response;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiModelCatalogResponse {
    private AiProvider provider;
    private String modelId;
    private String displayName;
    private String modelVersion;
    private Integer inputTokenLimit;
    private Integer outputTokenLimit;
    private List<String> supportedActions;
}
