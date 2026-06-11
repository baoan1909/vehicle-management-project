package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CustomerOnboardingApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.CustomerOnboardingApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.CustomerOnboardingApprovalAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerOnboardingApprovalApiMapper {

    CustomerOnboardingApprovalFilterCommand toFilterCommand(CustomerOnboardingApprovalFilterRequest request);

    ReviewInternalEmployeeApprovalCommand toReviewCommand(ReviewInternalEmployeeApprovalRequest request);

    CustomerOnboardingApprovalAdminResponse toResponse(CustomerOnboardingApprovalResult result);

    List<CustomerOnboardingApprovalAdminResponse> toResponses(List<CustomerOnboardingApprovalResult> results);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
