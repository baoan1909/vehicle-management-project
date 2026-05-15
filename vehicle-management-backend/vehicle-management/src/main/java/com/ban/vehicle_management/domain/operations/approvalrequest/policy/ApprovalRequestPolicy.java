package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;

public class ApprovalRequestPolicy {

    public void initialize(ApprovalRequest approvalRequest) {
        requireApprovalRequest(approvalRequest);
        approvalRequest.setRequestType(normalizeRequired(approvalRequest.getRequestType(), "requestType"));
        approvalRequest.setTargetSchema(normalizeRequired(approvalRequest.getTargetSchema(), "targetSchema"));
        approvalRequest.setTargetTable(normalizeRequired(approvalRequest.getTargetTable(), "targetTable"));
        requireField(approvalRequest.getTargetId(), "targetId");
        approvalRequest.setNote(normalizeNullable(approvalRequest.getNote()));
        if (approvalRequest.getStatus() == null) {
            approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        }
        validateState(approvalRequest);
    }

    public void approve(ApprovalRequest approvalRequest, UUID approvedBy, Instant approvedAt) {
        requireStatus(approvalRequest, ApprovalRequestStatus.PENDING);
        requireField(approvedBy, "approvedBy");
        requireField(approvedAt, "approvedAt");

        approvalRequest.setStatus(ApprovalRequestStatus.APPROVED);
        approvalRequest.setApprovedBy(approvedBy);
        approvalRequest.setApprovedAt(approvedAt);
        validateState(approvalRequest);
    }

    public void reject(ApprovalRequest approvalRequest, String note) {
        requireStatus(approvalRequest, ApprovalRequestStatus.PENDING);

        approvalRequest.setStatus(ApprovalRequestStatus.REJECTED);
        approvalRequest.setApprovedBy(null);
        approvalRequest.setApprovedAt(null);
        approvalRequest.setNote(normalizeNullable(note));
        validateState(approvalRequest);
    }

    public void cancel(ApprovalRequest approvalRequest, String note) {
        requireStatus(approvalRequest, ApprovalRequestStatus.PENDING);

        approvalRequest.setStatus(ApprovalRequestStatus.CANCELLED);
        approvalRequest.setApprovedBy(null);
        approvalRequest.setApprovedAt(null);
        approvalRequest.setNote(normalizeNullable(note));
        validateState(approvalRequest);
    }

    public void validateState(ApprovalRequest approvalRequest) {
        requireApprovalRequest(approvalRequest);
        approvalRequest.setRequestType(normalizeRequired(approvalRequest.getRequestType(), "requestType"));
        approvalRequest.setTargetSchema(normalizeRequired(approvalRequest.getTargetSchema(), "targetSchema"));
        approvalRequest.setTargetTable(normalizeRequired(approvalRequest.getTargetTable(), "targetTable"));
        requireField(approvalRequest.getTargetId(), "targetId");
        requireField(approvalRequest.getStatus(), "status");
        approvalRequest.setNote(normalizeNullable(approvalRequest.getNote()));

        boolean hasApprovalMetadata = approvalRequest.getApprovedBy() != null || approvalRequest.getApprovedAt() != null;
        boolean hasFullApprovalMetadata = approvalRequest.getApprovedBy() != null && approvalRequest.getApprovedAt() != null;

        if (approvalRequest.getStatus() == ApprovalRequestStatus.APPROVED) {
            if (!hasFullApprovalMetadata) {
                throw new BadRequestException("Approved request must have approvedBy and approvedAt");
            }
            return;
        }

        if (hasApprovalMetadata) {
            throw new BadRequestException("Only approved request can keep approvedBy and approvedAt");
        }
    }

    private void requireStatus(ApprovalRequest approvalRequest, ApprovalRequestStatus expectedStatus) {
        requireApprovalRequest(approvalRequest);
        if (approvalRequest.getStatus() != expectedStatus) {
            throw new BadRequestException("Approval request must be in " + expectedStatus + " status");
        }
    }

    private void requireApprovalRequest(ApprovalRequest approvalRequest) {
        requireField(approvalRequest, "approvalRequest");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalizedValue = normalizeNullable(value);
        if (normalizedValue == null) {
            throw new BadRequestException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}

