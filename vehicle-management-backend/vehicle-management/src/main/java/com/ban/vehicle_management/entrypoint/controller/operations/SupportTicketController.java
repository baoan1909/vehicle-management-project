package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.supportticket.mapper.SupportTicketApiMapper;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.AssignSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.CreateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.ResolveSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.SupportTicketFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.UpdateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/support-tickets")
public class SupportTicketController {

    private final SupportTicketPortIn supportTicketPortIn;
    private final SupportTicketApiMapper supportTicketApiMapper;

    public SupportTicketController(
            SupportTicketPortIn supportTicketPortIn,
            SupportTicketApiMapper supportTicketApiMapper
    ) {
        this.supportTicketPortIn = supportTicketPortIn;
        this.supportTicketApiMapper = supportTicketApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> createTicket(
            @RequestBody CreateSupportTicketRequest request
    ) {
        SupportTicket createdTicket = supportTicketPortIn.createTicket(supportTicketApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Support ticket created successfully",
                supportTicketApiMapper.toAdminResponse(createdTicket)
        ));
    }

    @GetMapping("/{supportTicketId}")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> getTicketById(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicket supportTicket = supportTicketPortIn.getTicketById(supportTicketId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched support ticket successfully",
                supportTicketApiMapper.toAdminResponse(supportTicket)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportTicketAdminResponse>>> getTickets(
            @ModelAttribute SupportTicketFilterRequest request
    ) {
        List<SupportTicket> supportTickets = supportTicketPortIn.getTickets(
                request.customerId(),
                request.categoryId(),
                request.assignedTo(),
                request.status(),
                request.priority(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched support tickets successfully",
                supportTicketApiMapper.toAdminResponses(supportTickets)
        ));
    }

    @PutMapping("/{supportTicketId}")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> updateTicket(
            @PathVariable UUID supportTicketId,
            @RequestBody UpdateSupportTicketRequest request
    ) {
        SupportTicket updatedTicket = supportTicketPortIn.updateTicket(
                supportTicketId,
                supportTicketApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket updated successfully",
                supportTicketApiMapper.toAdminResponse(updatedTicket)
        ));
    }

    @PatchMapping("/{supportTicketId}/assign")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> assignTicket(
            @PathVariable UUID supportTicketId,
            @RequestBody AssignSupportTicketRequest request
    ) {
        SupportTicket assignedTicket = supportTicketPortIn.assignTicket(supportTicketId, request.assignedTo());

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket assigned successfully",
                supportTicketApiMapper.toAdminResponse(assignedTicket)
        ));
    }

    @PatchMapping("/{supportTicketId}/start-progress")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> startProgress(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicket supportTicket = supportTicketPortIn.startProgress(supportTicketId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket started progress successfully",
                supportTicketApiMapper.toAdminResponse(supportTicket)
        ));
    }

    @PatchMapping("/{supportTicketId}/resolve")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> resolveTicket(
            @PathVariable UUID supportTicketId,
            @RequestBody ResolveSupportTicketRequest request
    ) {
        SupportTicket supportTicket = supportTicketPortIn.resolveTicket(supportTicketId, request.resolutionNote());

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket resolved successfully",
                supportTicketApiMapper.toAdminResponse(supportTicket)
        ));
    }

    @PatchMapping("/{supportTicketId}/reopen")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> reopenTicket(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicket supportTicket = supportTicketPortIn.reopenTicket(supportTicketId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket reopened successfully",
                supportTicketApiMapper.toAdminResponse(supportTicket)
        ));
    }

    @PatchMapping("/{supportTicketId}/close")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> closeTicket(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicket supportTicket = supportTicketPortIn.closeTicket(supportTicketId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket closed successfully",
                supportTicketApiMapper.toAdminResponse(supportTicket)
        ));
    }
}