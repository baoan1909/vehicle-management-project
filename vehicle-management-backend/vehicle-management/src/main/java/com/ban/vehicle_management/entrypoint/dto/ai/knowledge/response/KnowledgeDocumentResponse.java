package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDocumentResponse {
    private UUID documentId;
    private UUID sourceId;
    private UUID documentKey;
    private UUID tenantId;
    private String title;
    private String originalFilename;
    private String fileExtension;
    private String mimeType;
    private Long fileSizeBytes;
    private String checksumSha256;
    private Integer documentVersion;
    private KnowledgeAccessScope accessScope;
    private KnowledgeDocumentStatus status;
    private String failureCode;
    private String effectiveFrom;
    private String effectiveTo;
    private String reviewedAt;
    private String archivedAt;
    private String createdAt;
    private String updatedAt;
}