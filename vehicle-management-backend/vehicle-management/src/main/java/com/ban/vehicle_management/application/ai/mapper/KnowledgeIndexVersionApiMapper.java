package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeIndexVersionCommand;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.entrypoint.dto.ai.indexversion.request.CreateKnowledgeIndexVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.indexversion.response.KnowledgeIndexVersionResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeIndexVersionApiMapper {

    CreateKnowledgeIndexVersionCommand toCommand(CreateKnowledgeIndexVersionRequest request);

    KnowledgeIndexVersionResponse toResponse(KnowledgeIndexVersion indexVersion);

    List<KnowledgeIndexVersionResponse> toResponses(List<KnowledgeIndexVersion> indexVersions);

}
