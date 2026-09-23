package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.KnowledgeQualityApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeQualityPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeQualityDashboardResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/quality-dashboard")
public class KnowledgeQualityController {

    private final KnowledgeQualityPortIn qualityPortIn;
    private final KnowledgeQualityApiMapper mapper;

    public KnowledgeQualityController(
            KnowledgeQualityPortIn qualityPortIn,
            KnowledgeQualityApiMapper mapper
    ) {
        this.qualityPortIn = qualityPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeQualityDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy tổng quan chất lượng tri thức thành công",
                mapper.toResponse(qualityPortIn.summarize())
        ));
    }
}