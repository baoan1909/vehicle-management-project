package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.application.ai.command.UploadKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeChunkResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeDocumentBlockResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeDocumentResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.UploadKnowledgeDocumentRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeDocumentFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface KnowledgeDocumentApiMapper {

    UploadKnowledgeDocumentCommand toUploadCommand(UploadKnowledgeDocumentRequest request);

    KnowledgeDocumentQuery toQuery(KnowledgeDocumentFilterRequest request);

    KnowledgeDocumentResponse toResponse(KnowledgeDocument document);

    List<KnowledgeDocumentResponse> toResponses(List<KnowledgeDocument> documents);

    default PageResponse<KnowledgeDocumentResponse> toPageResponse(Page<KnowledgeDocument> page) {
        return new PageResponse<>(toResponses(page.getContent()), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    KnowledgeDocumentBlockResponse toBlockResponse(KnowledgeDocumentBlock block);

    List<KnowledgeDocumentBlockResponse> toBlockResponses(List<KnowledgeDocumentBlock> blocks);

    KnowledgeChunkResponse toChunkResponse(KnowledgeChunkSnapshot chunk);

    List<KnowledgeChunkResponse> toChunkResponses(List<KnowledgeChunkSnapshot> chunks);

}
