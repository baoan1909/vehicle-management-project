package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.query.KnowledgeIngestionJobQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeIngestionJobEventResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeIngestionJobResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeIngestionJobFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface KnowledgeIngestionJobApiMapper {

    KnowledgeIngestionJobQuery toQuery(KnowledgeIngestionJobFilterRequest request);

    KnowledgeIngestionJobResponse toResponse(KnowledgeIngestionJob job);

    List<KnowledgeIngestionJobResponse> toResponses(List<KnowledgeIngestionJob> jobs);

    default PageResponse<KnowledgeIngestionJobResponse> toPageResponse(Page<KnowledgeIngestionJob> page) {
        return new PageResponse<>(toResponses(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    KnowledgeIngestionJobEventResponse toEventResponse(KnowledgeIngestionJobEvent event);

    List<KnowledgeIngestionJobEventResponse> toEventResponses(List<KnowledgeIngestionJobEvent> events);

}
