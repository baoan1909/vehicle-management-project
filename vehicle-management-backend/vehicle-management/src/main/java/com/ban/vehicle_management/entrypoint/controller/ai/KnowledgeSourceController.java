package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.KnowledgeDocumentApiMapper;
import com.ban.vehicle_management.application.ai.mapper.KnowledgeSourceApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeDocumentPortIn;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeSourcePortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.CreateKnowledgeSourceRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.UpdateKnowledgeSourceRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeSourceFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeDocumentResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeSourceResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/sources")
public class KnowledgeSourceController {

    private final KnowledgeSourcePortIn sourcePortIn;
    private final KnowledgeDocumentPortIn documentPortIn;
    private final KnowledgeSourceApiMapper mapper;
    private final KnowledgeDocumentApiMapper documentMapper;

    public KnowledgeSourceController(
            KnowledgeSourcePortIn sourcePortIn,
            KnowledgeDocumentPortIn documentPortIn,
            KnowledgeSourceApiMapper mapper,
            KnowledgeDocumentApiMapper documentMapper
    ) {
        this.sourcePortIn = sourcePortIn;
        this.documentPortIn = documentPortIn;
        this.mapper = mapper;
        this.documentMapper = documentMapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeSourceResponse>>> listSources(
            @ModelAttribute KnowledgeSourceFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách nguồn kiến thức thành công",
                mapper.toPageResponse(sourcePortIn.listSources(mapper.toQuery(filter), pageable))
        ));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeSourceResponse>> createSource(
            @Valid @RequestBody CreateKnowledgeSourceRequest request
    ) {
        KnowledgeSourceResponse response = mapper.toResponse(
                sourcePortIn.createSource(mapper.toCreateCommand(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Tạo nguồn kiến thức thành công",
                response
        ));
    }

    @PutMapping("/{sourceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeSourceResponse>> updateSource(
            @PathVariable UUID sourceId,
            @Valid @RequestBody UpdateKnowledgeSourceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật nguồn kiến thức thành công",
                mapper.toResponse(sourcePortIn.updateSource(sourceId, mapper.toUpdateCommand(request)))
        ));
    }

    @PatchMapping("/{sourceId}/deactivate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deactivateSource(@PathVariable UUID sourceId) {
        sourcePortIn.deactivateSource(sourceId);
        return ResponseEntity.ok(ApiResponse.ok("Ngừng hoạt động nguồn kiến thức thành công"));
    }

    @PatchMapping("/{sourceId}/reactivate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> reactivateSource(@PathVariable UUID sourceId) {
        sourcePortIn.reactivateSource(sourceId);
        return ResponseEntity.ok(ApiResponse.ok("Kích hoạt lại nguồn kiến thức thành công"));
    }

    @GetMapping("/{sourceId}/documents")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeDocumentResponse>>> listDocuments(
            @PathVariable UUID sourceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách tài liệu của nguồn thành công",
                documentMapper.toPageResponse(sourcePortIn.listDocuments(sourceId, pageable))
        ));
    }
}
