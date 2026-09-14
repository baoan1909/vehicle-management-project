package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.AiModelAdminApiMapper;
import com.ban.vehicle_management.application.ai.port.in.AiModelAdminPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.model.request.UpdateAiModelRolloutRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.model.request.UpdateAiModelStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelCatalogResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelConfigurationAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelWarningResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/models")
public class AiModelAdminController {

    private final AiModelAdminPortIn aiModelAdminPortIn;
    private final AiModelAdminApiMapper mapper;

    public AiModelAdminController(AiModelAdminPortIn aiModelAdminPortIn, AiModelAdminApiMapper mapper) {
        this.aiModelAdminPortIn = aiModelAdminPortIn;
        this.mapper = mapper;
    }

    @GetMapping("/configurations")
    public ResponseEntity<ApiResponse<List<AiModelConfigurationAdminResponse>>> listConfigurations() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched AI model configurations successfully",
                mapper.toConfigurationResponses(aiModelAdminPortIn.listConfigurations())
        ));
    }

    @PatchMapping("/configurations/{configurationId}/status")
    public ResponseEntity<ApiResponse<AiModelConfigurationAdminResponse>> updateStatus(
            @PathVariable UUID configurationId,
            @RequestBody UpdateAiModelStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "AI model status updated successfully",
                mapper.toConfigurationResponse(aiModelAdminPortIn.updateStatus(configurationId, request.status()))
        ));
    }

    @PatchMapping("/configurations/{configurationId}/rollout")
    public ResponseEntity<ApiResponse<AiModelConfigurationAdminResponse>> updateRollout(
            @PathVariable UUID configurationId,
            @RequestBody UpdateAiModelRolloutRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "AI model rollout updated successfully",
                mapper.toConfigurationResponse(aiModelAdminPortIn.updateRollout(configurationId, request.rolloutPercentage()))
        ));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<AiModelCatalogResponse>>> listCatalog() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched AI model catalog successfully",
                mapper.toCatalogResponses(aiModelAdminPortIn.listCatalog())
        ));
    }

    @PostMapping("/catalog/sync")
    public ResponseEntity<ApiResponse<List<AiModelCatalogResponse>>> syncCatalog() {
        return ResponseEntity.ok(ApiResponse.ok(
                "AI model catalog synced successfully",
                mapper.toCatalogResponses(aiModelAdminPortIn.syncCatalog())
        ));
    }

    @GetMapping("/warnings")
    public ResponseEntity<ApiResponse<List<AiModelWarningResponse>>> listWarnings() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched AI model warnings successfully",
                mapper.toWarningResponses(aiModelAdminPortIn.listWarnings())
        ));
    }
}
