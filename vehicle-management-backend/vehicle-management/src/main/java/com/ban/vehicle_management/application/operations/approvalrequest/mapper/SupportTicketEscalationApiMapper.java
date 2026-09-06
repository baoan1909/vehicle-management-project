package com.ban.vehicle_management.application.operations.approvalrequest.mapper;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CreateSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SupportTicketEscalationResult;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.CreateSupportTicketEscalationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewSupportTicketEscalationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.SupportTicketEscalationResponse;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketEscalationApiMapper {

    @Mapping(target = "reasonCode", source = "request.reasonCode")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    CreateSupportTicketEscalationCommand toCreateCommand(
            CreateSupportTicketEscalationRequest request,
            String idempotencyKey
    );

    ReviewSupportTicketEscalationCommand toReviewCommand(ReviewSupportTicketEscalationRequest request);

    SupportTicketEscalationResponse toResponse(SupportTicketEscalationResult result);

    List<SupportTicketEscalationResponse> toResponses(List<SupportTicketEscalationResult> results);

    default String map(Instant instant) {
        return DateTimeUtils.formatInstant(instant, DateTimeUtils.VIETNAM_ZONE);
    }
}
