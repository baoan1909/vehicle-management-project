package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.SystemAdminApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.SystemAdminApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.SystemAdminApprovalAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SystemAdminApprovalApiMapper {

    SystemAdminApprovalFilterCommand toFilterCommand(SystemAdminApprovalFilterRequest request);

    ReviewInternalEmployeeApprovalCommand toReviewCommand(ReviewInternalEmployeeApprovalRequest request);

    SystemAdminApprovalAdminResponse toResponse(SystemAdminApprovalResult result);

    List<SystemAdminApprovalAdminResponse> toResponses(List<SystemAdminApprovalResult> results);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
