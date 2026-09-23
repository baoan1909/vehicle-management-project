package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.KnowledgeRetrievalApiMapper;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeRetrievalPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeRetrievalTestResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/knowledge/retrieval-tests")
public class KnowledgeRetrievalTestController {

    private final KnowledgeRetrievalPortIn retrievalPortIn;
    private final KnowledgeRetrievalApiMapper mapper;

    public KnowledgeRetrievalTestController(
            KnowledgeRetrievalPortIn retrievalPortIn,
            KnowledgeRetrievalApiMapper mapper
    ) {
        this.retrievalPortIn = retrievalPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_KNOWLEDGE_READ_ALL')")
    public ResponseEntity<ApiResponse<KnowledgeRetrievalTestResponse>> testSearch(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Tìm kiếm thử nghiệm thành công",
                mapper.toResponse(retrievalPortIn.searchForCurrentUser(query, limit))
        ));
    }
}