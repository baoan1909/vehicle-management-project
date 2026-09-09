package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalSummaryResult;

public interface OnboardingApprovalSummaryPortIn {

    OnboardingApprovalSummaryResult getMyPendingSummary();
}
