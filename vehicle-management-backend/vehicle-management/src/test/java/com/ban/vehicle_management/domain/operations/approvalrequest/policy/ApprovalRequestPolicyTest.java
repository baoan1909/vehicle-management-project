package com.ban.vehicle_management.domain.operations.approvalrequest.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApprovalRequestPolicyTest {

    private final ApprovalRequestPolicy approvalRequestPolicy = new ApprovalRequestPolicy();

    @Test
    void shouldInitializeApprovalRequestWithPendingStatus() {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setRequestType(" SUBSCRIPTION_REGISTER ");
        approvalRequest.setTargetSchema(" access_control ");
        approvalRequest.setTargetTable(" subscriptions ");
        approvalRequest.setTargetId(UUID.randomUUID());

        approvalRequestPolicy.initialize(approvalRequest);

        assertEquals("SUBSCRIPTION_REGISTER", approvalRequest.getRequestType());
        assertEquals("access_control", approvalRequest.getTargetSchema());
        assertEquals("subscriptions", approvalRequest.getTargetTable());
        assertEquals(ApprovalRequestStatus.PENDING, approvalRequest.getStatus());
    }

    @Test
    void shouldApprovePendingRequest() {
        ApprovalRequest approvalRequest = validApprovalRequest();
        UUID approvedBy = UUID.randomUUID();
        Instant approvedAt = Instant.parse("2026-05-15T02:00:00Z");

        approvalRequestPolicy.approve(approvalRequest, approvedBy, approvedAt);

        assertEquals(ApprovalRequestStatus.APPROVED, approvalRequest.getStatus());
        assertEquals(approvedBy, approvalRequest.getApprovedBy());
        assertEquals(approvedAt, approvalRequest.getApprovedAt());
    }

    @Test
    void shouldRejectPendingRequestAndClearApprovalMetadata() {
        ApprovalRequest approvalRequest = validApprovalRequest();
        approvalRequestPolicy.reject(approvalRequest, "Khong hop le");

        assertEquals(ApprovalRequestStatus.REJECTED, approvalRequest.getStatus());
        assertNull(approvalRequest.getApprovedBy());
        assertNull(approvalRequest.getApprovedAt());
    }

    @Test
    void shouldRejectNonApprovedStatusWithApprovalMetadata() {
        ApprovalRequest approvalRequest = validApprovalRequest();
        approvalRequest.setApprovedBy(UUID.randomUUID());

        assertThrows(BadRequestException.class, () -> approvalRequestPolicy.validateState(approvalRequest));
    }

    private ApprovalRequest validApprovalRequest() {
        ApprovalRequest approvalRequest = new ApprovalRequest();
        approvalRequest.setApprovalRequestId(UUID.randomUUID());
        approvalRequest.setRequestType("SUBSCRIPTION_REGISTER");
        approvalRequest.setTargetSchema("access_control");
        approvalRequest.setTargetTable("subscriptions");
        approvalRequest.setTargetId(UUID.randomUUID());
        approvalRequest.setStatus(ApprovalRequestStatus.PENDING);
        return approvalRequest;
    }
}

