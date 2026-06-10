package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.InternalEmployeeApprovalFilterCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewInternalEmployeeApprovalCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.InternalEmployeeApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.InternalEmployeeApprovalAdminResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InternalEmployeeApprovalApiMapper {

    InternalEmployeeApprovalFilterCommand toFilterCommand(InternalEmployeeApprovalFilterRequest request);

    ReviewInternalEmployeeApprovalCommand toReviewCommand(ReviewInternalEmployeeApprovalRequest request);

    InternalEmployeeApprovalAdminResponse toResponse(InternalEmployeeApprovalResult result);

    List<InternalEmployeeApprovalAdminResponse> toResponses(List<InternalEmployeeApprovalResult> results);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
