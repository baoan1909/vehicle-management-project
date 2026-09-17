package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchHit;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeRetrievalTestResponse {
    private String diagnosticCode;
    private UUID activeIndexVersionId;
    private List<KnowledgeSearchHit> results;
}