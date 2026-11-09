package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.UpdateOnboardingApprovalPoliciesCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalPoliciesResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.UpdateOnboardingApprovalPoliciesRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.OnboardingApprovalPoliciesResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OnboardingApprovalPolicyApiMapper {

    UpdateOnboardingApprovalPoliciesCommand toCommand(UpdateOnboardingApprovalPoliciesRequest request);

    OnboardingApprovalPoliciesResponse toResponse(OnboardingApprovalPoliciesResult result);
}
