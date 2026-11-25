package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.CommonKnowledgeCandidate;
import java.util.List;
import java.util.UUID;

public interface CommonKnowledgePortOut {
    List<CommonKnowledgeCandidate> searchReviewedFaqs(UUID tenantId, List<String> scopes, String tsQuery, int limit);
    /** Recheck approval/version/permission and lock evidence for the publishing transaction. */
    List<CommonKnowledgeCandidate> lockReviewedFaqs(UUID tenantId, List<String> scopes, List<UUID> chunkIds);
}
