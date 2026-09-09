package com.ban.vehicle_management.domain.operations.approvalrequest.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest extends AuditableDomainModel {

    private UUID approvalRequestId;
    private String requestType;
    private String targetSchema;
    private String targetTable;
    private UUID targetId;
    private ApprovalRequestStatus status;
    private UUID requestedBy;
    private UUID approvedBy;
    private Instant approvedAt;
    private String note;
    private String idempotencyKey;
    private Map<String, String> requestData;
    private Map<String, String> decisionData;
}

