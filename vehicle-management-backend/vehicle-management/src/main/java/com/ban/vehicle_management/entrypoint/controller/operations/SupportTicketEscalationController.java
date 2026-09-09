package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.SupportTicketEscalationApiMapper;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.SupportTicketEscalationPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.CreateSupportTicketEscalationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.RejectSupportTicketEscalationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewSupportTicketEscalationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.SupportTicketEscalationResponse;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class SupportTicketEscalationController {

    private final SupportTicketEscalationPortIn portIn;
    private final SupportTicketEscalationApiMapper mapper;

    public SupportTicketEscalationController(
            SupportTicketEscalationPortIn portIn,
            SupportTicketEscalationApiMapper mapper
    ) {
        this.portIn = portIn;
        this.mapper = mapper;
    }

    @PostMapping("/support-tickets/{supportTicketId}/escalations")
    public ResponseEntity<ApiResponse<SupportTicketEscalationResponse>> create(
            @PathVariable UUID supportTicketId,
            @RequestBody CreateSupportTicketEscalationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Support ticket escalation created successfully",
                mapper.toResponse(portIn.create(supportTicketId, mapper.toCreateCommand(request, idempotencyKey)))
        ));
    }

    @GetMapping("/support-tickets/{supportTicketId}/escalations/mine/current")
    public ResponseEntity<ApiResponse<SupportTicketEscalationResponse>> getMyCurrent(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicketEscalationResponse response = portIn.getMyCurrent(supportTicketId)
                .map(mapper::toResponse)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Current support ticket escalation fetched successfully", response));
    }

    @GetMapping("/support-ticket-escalations")
    public ResponseEntity<ApiResponse<List<SupportTicketEscalationResponse>>> getAll(
            @RequestParam(required = false) ApprovalRequestStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket escalations fetched successfully",
                mapper.toResponses(portIn.getAll(status))
        ));
    }

    @PatchMapping("/support-ticket-escalations/{escalationId}/approve")
    public ResponseEntity<ApiResponse<SupportTicketEscalationResponse>> approve(
            @PathVariable UUID escalationId,
            @RequestBody ReviewSupportTicketEscalationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket escalation approved successfully",
                mapper.toResponse(portIn.approve(escalationId, mapper.toReviewCommand(request)))
        ));
    }

    @PatchMapping("/support-ticket-escalations/{escalationId}/reject")
    public ResponseEntity<ApiResponse<SupportTicketEscalationResponse>> reject(
            @PathVariable UUID escalationId,
            @RequestBody RejectSupportTicketEscalationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket escalation rejected successfully",
                mapper.toResponse(portIn.reject(escalationId, request == null ? null : request.note()))
        ));
    }
}
