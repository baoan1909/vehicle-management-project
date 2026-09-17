package com.ban.vehicle_management.application.ai.mapper;

import com.ban.vehicle_management.application.ai.service.KnowledgeQualityService.KnowledgeQualityDashboard;
import com.ban.vehicle_management.application.ai.service.KnowledgeQualityService.KnowledgeQualityWarning;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeQualityDashboardResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response.KnowledgeQualityWarningResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface KnowledgeQualityApiMapper {

    default KnowledgeQualityDashboardResponse toResponse(KnowledgeQualityDashboard dashboard) {
        KnowledgeQualityDashboardResponse response = new KnowledgeQualityDashboardResponse();
        response.setSourcesByStatus(dashboard.sourcesByStatus());
        response.setDocumentsByStatus(dashboard.documentsByStatus());
        response.setJobsByStatus(dashboard.jobsByStatus());
        response.setIndexVersionsByStatus(dashboard.indexVersionsByStatus());
        response.setActiveIndexCount(dashboard.activeIndexCount());
        response.setTotalIndexVersionCount(dashboard.totalIndexVersionCount());
        response.setWarnings(toWarnings(dashboard.warnings()));
        return response;
    }

    private List<KnowledgeQualityWarningResponse> toWarnings(List<KnowledgeQualityWarning> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings.stream()
                .map(warning -> new KnowledgeQualityWarningResponse(
                        warning.code(), warning.severity(), warning.count(), warning.message()))
                .toList();
    }
}