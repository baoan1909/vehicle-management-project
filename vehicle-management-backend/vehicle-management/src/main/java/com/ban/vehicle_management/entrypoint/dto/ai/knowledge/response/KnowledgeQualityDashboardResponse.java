package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeQualityDashboardResponse {
    private Map<String, Long> sourcesByStatus;
    private Map<String, Long> documentsByStatus;
    private Map<String, Long> jobsByStatus;
    private Map<String, Long> indexVersionsByStatus;
    private long activeIndexCount;
    private long totalIndexVersionCount;
    private List<KnowledgeQualityWarningResponse> warnings;
}