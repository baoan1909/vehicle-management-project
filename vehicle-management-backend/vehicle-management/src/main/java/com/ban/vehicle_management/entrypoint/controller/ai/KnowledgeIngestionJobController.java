package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.KnowledgeIngestionJobApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeIngestionJobPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeIngestionJobEventResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeIngestionJobResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request.KnowledgeIngestionJobFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/ingestion-jobs")
public class KnowledgeIngestionJobController {

    private final KnowledgeIngestionJobPortIn jobPortIn;
    private final KnowledgeIngestionJobApiMapper mapper;

    public KnowledgeIngestionJobController(
            KnowledgeIngestionJobPortIn jobPortIn,
            KnowledgeIngestionJobApiMapper mapper
    ) {
        this.jobPortIn = jobPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<KnowledgeIngestionJobResponse>>> listJobs(
            @ModelAttribute KnowledgeIngestionJobFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách công việc xử lý thành công",
                mapper.toPageResponse(jobPortIn.listJobs(mapper.toQuery(filter), pageable))
        ));
    }

    @GetMapping("/{ingestionJobId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeIngestionJobResponse>> jobDetail(
            @PathVariable UUID ingestionJobId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy thông tin công việc xử lý thành công",
                mapper.toResponse(jobPortIn.jobDetail(ingestionJobId))
        ));
    }

    @GetMapping("/{ingestionJobId}/events")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<KnowledgeIngestionJobEventResponse>>> jobEvents(
            @PathVariable UUID ingestionJobId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy lịch sử xử lý thành công",
                mapper.toEventResponses(jobPortIn.jobEvents(ingestionJobId))
        ));
    }

    @PostMapping("/{ingestionJobId}/retry")
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeIngestionJobResponse>> retryJob(
            @PathVariable UUID ingestionJobId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã đưa công việc vào hàng đợi xử lý lại",
                mapper.toResponse(jobPortIn.retryJob(ingestionJobId))
        ));
    }
}
