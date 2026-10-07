package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.UpdateOnboardingApprovalPoliciesCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalPoliciesResult;

public interface OnboardingApprovalPolicyPortIn {

    OnboardingApprovalPoliciesResult getPolicies();

    OnboardingApprovalPoliciesResult updatePolicies(UpdateOnboardingApprovalPoliciesCommand command);
}
