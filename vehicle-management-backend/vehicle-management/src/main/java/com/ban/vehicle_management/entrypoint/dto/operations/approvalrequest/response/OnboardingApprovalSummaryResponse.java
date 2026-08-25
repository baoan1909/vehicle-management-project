package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

public record OnboardingApprovalSummaryResponse(
        long totalPending,
        long systemAdminPending,
        long internalEmployeePending,
        long customerPending
) {
}
