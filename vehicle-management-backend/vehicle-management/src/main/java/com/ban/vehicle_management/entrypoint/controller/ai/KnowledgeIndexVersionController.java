package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.KnowledgeIndexVersionApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeIndexVersionPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.indexversion.request.CreateKnowledgeIndexVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.indexversion.response.KnowledgeIndexVersionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/index-versions")
public class KnowledgeIndexVersionController {

    private final KnowledgeIndexVersionPortIn indexVersionPortIn;
    private final KnowledgeIndexVersionApiMapper mapper;

    public KnowledgeIndexVersionController(
            KnowledgeIndexVersionPortIn indexVersionPortIn,
            KnowledgeIndexVersionApiMapper mapper
    ) {
        this.indexVersionPortIn = indexVersionPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<KnowledgeIndexVersionResponse>>> listIndexVersions() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách phiên bản chỉ mục tri thức thành công",
                mapper.toResponses(indexVersionPortIn.listIndexVersions())
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeIndexVersionResponse>> createDraft(
            @Valid @RequestBody CreateKnowledgeIndexVersionRequest request
    ) {
        KnowledgeIndexVersionResponse response = mapper.toResponse(
                indexVersionPortIn.createDraft(mapper.toCommand(request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Tạo phiên bản chỉ mục tri thức dự thảo thành công",
                response
        ));
    }

    @PostMapping("/{indexVersionId}/build")
    public ResponseEntity<ApiResponse<KnowledgeIndexVersionResponse>> startBuild(@PathVariable UUID indexVersionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã bắt đầu xây dựng chỉ mục tri thức",
                mapper.toResponse(indexVersionPortIn.startBuild(indexVersionId))
        ));
    }

    @PostMapping("/{indexVersionId}/activate")
    public ResponseEntity<ApiResponse<KnowledgeIndexVersionResponse>> activate(@PathVariable UUID indexVersionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Kích hoạt phiên bản chỉ mục tri thức thành công",
                mapper.toResponse(indexVersionPortIn.activate(indexVersionId))
        ));
    }

    @PostMapping("/{indexVersionId}/rollback")
    public ResponseEntity<ApiResponse<KnowledgeIndexVersionResponse>> rollback(@PathVariable UUID indexVersionId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Quay lại phiên bản chỉ mục tri thức thành công",
                mapper.toResponse(indexVersionPortIn.rollback(indexVersionId))
        ));
    }
}
