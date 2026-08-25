package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalSummaryResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.OnboardingApprovalSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OnboardingApprovalSummaryApiMapper {

    OnboardingApprovalSummaryResponse toResponse(OnboardingApprovalSummaryResult result);
}
