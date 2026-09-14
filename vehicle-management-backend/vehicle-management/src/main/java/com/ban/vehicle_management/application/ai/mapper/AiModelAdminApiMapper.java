package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelCatalogResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelConfigurationAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiModelWarningResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiModelAdminApiMapper {

    AiModelConfigurationAdminResponse toConfigurationResponse(AiModelConfiguration configuration);

    List<AiModelConfigurationAdminResponse> toConfigurationResponses(List<AiModelConfiguration> configurations);

    AiModelCatalogResponse toCatalogResponse(AiProviderModel model);

    List<AiModelCatalogResponse> toCatalogResponses(List<AiProviderModel> models);

    AiModelWarningResponse toWarningResponse(AiModelWarning warning);

    List<AiModelWarningResponse> toWarningResponses(List<AiModelWarning> warnings);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
