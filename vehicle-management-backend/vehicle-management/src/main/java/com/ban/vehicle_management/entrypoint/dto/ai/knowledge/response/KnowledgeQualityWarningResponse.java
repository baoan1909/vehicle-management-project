package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeQualityWarningResponse {
    private String code;
    private String severity;
    private Long count;
    private String message;
}