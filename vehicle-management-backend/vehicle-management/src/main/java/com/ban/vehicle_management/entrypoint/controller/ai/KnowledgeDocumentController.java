package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.command.ArchiveKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.PublishKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.ReindexKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.RejectKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.mapper.KnowledgeDocumentApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeDocumentPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeChunkResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.UploadKnowledgeDocumentRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeDocumentFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeDocumentBlockResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeDocumentResponse;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/knowledge/documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentPortIn documentPortIn;
    private final KnowledgeDocumentApiMapper mapper;

    public KnowledgeDocumentController(
            KnowledgeDocumentPortIn documentPortIn,
            KnowledgeDocumentApiMapper mapper
    ) {
        this.documentPortIn = documentPortIn;
        this.mapper = mapper;
    }

    @PostMapping("/upload")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> upload(
            @RequestParam("sourceId") UUID sourceId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.getSize() > KnowledgeDocumentFilePolicy.DEFAULT_MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Tệp tải lên vượt quá dung lượng cho phép");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Không thể đọc tệp tải lên");
        }
        String effectiveIdempotencyKey = idempotencyKey != null ? idempotencyKey : idempotencyKeyHeader;
        UploadKnowledgeDocumentRequest request = new UploadKnowledgeDocumentRequest(
                sourceId,
                title,
                file.getOriginalFilename(),
                file.getContentType(),
                content,
                effectiveIdempotencyKey
        );
        var result = documentPortIn.uploadDocument(mapper.toUploadCommand(request));
        KnowledgeDocumentResponse response = mapper.toResponse(result.document());
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(
                    "Tải lên tài liệu thành công, đang chờ xử lý",
                    response
            ));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "Tài liệu đã tồn tại trong hệ thống",
                response
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeDocumentResponse>>> list(
            @ModelAttribute KnowledgeDocumentFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách tài liệu thành công",
                mapper.toPageResponse(documentPortIn.listDocuments(mapper.toQuery(filter), pageable))
        ));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> detail(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy thông tin tài liệu thành công",
                mapper.toResponse(documentPortIn.documentDetail(documentId))
        ));
    }

    @GetMapping("/{documentId}/blocks")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<KnowledgeDocumentBlockResponse>>> blocks(
            @PathVariable UUID documentId,
            @RequestParam("documentVersion") int documentVersion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách khối văn bản thành công",
                mapper.toBlockResponses(documentPortIn.documentBlocks(documentId, documentVersion))
        ));
    }

    @GetMapping("/{documentId}/chunks")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<KnowledgeChunkResponse>>> chunks(
            @PathVariable UUID documentId,
            @RequestParam("documentVersion") int documentVersion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách đoạn chỉ mục thành công",
                mapper.toChunkResponses(documentPortIn.documentChunks(documentId, documentVersion))
        ));
    }

    @PostMapping("/{documentId}/publish")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> publish(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Duyệt tài liệu thành công",
                mapper.toResponse(documentPortIn.publishDocument(new PublishKnowledgeDocumentCommand(documentId)))
        ));
    }

    @PostMapping("/{documentId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> reject(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Từ chối tài liệu thành công",
                mapper.toResponse(documentPortIn.rejectDocument(new RejectKnowledgeDocumentCommand(documentId)))
        ));
    }

    @PostMapping("/{documentId}/archive")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> archive(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lưu trữ tài liệu thành công",
                mapper.toResponse(documentPortIn.archiveDocument(new ArchiveKnowledgeDocumentCommand(documentId)))
        ));
    }

    @PostMapping("/{documentId}/reindex")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_REINDEX_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> reindex(
            @PathVariable UUID documentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        KnowledgeDocument reindexed = documentPortIn.reindexDocument(new ReindexKnowledgeDocumentCommand(documentId, idempotencyKey));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(
                "Yêu cầu lập chỉ mục lại đã được chấp nhận",
                mapper.toResponse(reindexed)));
    }
}
