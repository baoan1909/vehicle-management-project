package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.command.UpdateKnowledgeSourceCommand;
import com.ban.vehicle_management.application.ai.query.KnowledgeSourceQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.CreateKnowledgeSourceRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.UpdateKnowledgeSourceRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeSourceFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeSourceResponse;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface KnowledgeSourceApiMapper {

    CreateKnowledgeSourceCommand toCreateCommand(CreateKnowledgeSourceRequest request);

    UpdateKnowledgeSourceCommand toUpdateCommand(UpdateKnowledgeSourceRequest request);

    KnowledgeSourceQuery toQuery(KnowledgeSourceFilterRequest request);

    KnowledgeSourceResponse toResponse(KnowledgeSource source);

    List<KnowledgeSourceResponse> toResponses(List<KnowledgeSource> sources);

    default PageResponse<KnowledgeSourceResponse> toPageResponse(Page<KnowledgeSource> page) {
        return new PageResponse<>(toResponses(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
