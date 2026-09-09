package com.ban.vehicle_management.application.operations.approvalrequest.model.result;

public record OnboardingApprovalSummaryResult(
        long totalPending,
        long systemAdminPending,
        long internalEmployeePending,
        long customerPending
) {
}
