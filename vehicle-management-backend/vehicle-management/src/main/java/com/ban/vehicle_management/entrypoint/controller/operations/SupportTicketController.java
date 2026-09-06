package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.supportticket.mapper.SupportTicketApiMapper;
import com.ban.vehicle_management.application.operations.supportticket.mapper.SupportTicketChatIntakeApiMapper;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatConversationApiMapper;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.AssignSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.CreateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.ResolveSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.SupportTicketFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.request.UpdateSupportTicketRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response.SupportTicketChatIntakeResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatConversationUserResponse;
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
    private final SupportTicketChatIntakeApiMapper supportTicketChatIntakeApiMapper;
    private final ChatConversationApiMapper chatConversationApiMapper;

    public SupportTicketController(
            SupportTicketPortIn supportTicketPortIn,
            SupportTicketApiMapper supportTicketApiMapper,
            SupportTicketChatIntakeApiMapper supportTicketChatIntakeApiMapper,
            ChatConversationApiMapper chatConversationApiMapper
    ) {
        this.supportTicketPortIn = supportTicketPortIn;
        this.supportTicketApiMapper = supportTicketApiMapper;
        this.supportTicketChatIntakeApiMapper = supportTicketChatIntakeApiMapper;
        this.chatConversationApiMapper = chatConversationApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> createTicket(
            @RequestBody CreateSupportTicketRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        SupportTicket createdTicket = supportTicketPortIn.createTicket(
                supportTicketApiMapper.toDomain(request), idempotencyKey
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Support ticket created successfully",
                supportTicketApiMapper.toAdminResponse(createdTicket)
        ));
    }

    @PostMapping("/chat-intake")
    public ResponseEntity<ApiResponse<SupportTicketChatIntakeResponse>> createChatIntake(
            @RequestBody CreateSupportTicketRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        SupportTicketChatIntake intake = supportTicketPortIn.createChatIntake(
                supportTicketApiMapper.toDomain(request), idempotencyKey
        );
        return ResponseEntity.status(intake.reusedActiveTicket() ? HttpStatus.OK : HttpStatus.CREATED).body(ApiResponse.ok(
                intake.reusedActiveTicket()
                        ? "Active support conversation opened successfully"
                        : "Support chat intake created successfully",
                supportTicketChatIntakeApiMapper.toResponse(intake)
        ));
    }

    @GetMapping("/assistant-conversation")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> openAssistantConversation() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Support assistant conversation opened successfully",
                chatConversationApiMapper.toConversationUserResponse(supportTicketPortIn.openAssistantConversation())
        ));
    }

    @PostMapping("/from-conversations/{conversationId}")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> createTicketFromConversation(
            @PathVariable UUID conversationId,
            @RequestBody CreateSupportTicketRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        SupportTicket createdTicket = supportTicketPortIn.createTicketFromConversation(
                supportTicketApiMapper.toDomain(request),
                conversationId,
                idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Support ticket created from conversation successfully",
                supportTicketApiMapper.toAdminResponse(createdTicket)
        ));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<SupportTicketAdminResponse>>> getMyTickets(
            @RequestParam(required = false) com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched current customer support tickets successfully",
                supportTicketApiMapper.toAdminResponses(supportTicketPortIn.getMyTickets(status, keyword))
        ));
    }

    @GetMapping("/conversations/{conversationId}/history")
    public ResponseEntity<ApiResponse<List<SupportTicketAdminResponse>>> getConversationTicketHistory(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched conversation support ticket history successfully",
                supportTicketApiMapper.toAdminResponses(
                        supportTicketPortIn.getConversationTicketHistory(conversationId, status, keyword)
                )
        ));
    }

    @PostMapping("/assistant-conversation/tickets/{supportTicketId}")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> shareTicketWithAssistant(
            @PathVariable UUID supportTicketId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket attached to assistant conversation successfully",
                supportTicketApiMapper.toAdminResponse(supportTicketPortIn.shareTicketWithAssistant(supportTicketId))
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

    @PatchMapping("/{supportTicketId}/claim")
    public ResponseEntity<ApiResponse<SupportTicketAdminResponse>> claimTicket(
            @PathVariable UUID supportTicketId
    ) {
        SupportTicket claimedTicket = supportTicketPortIn.claimTicket(supportTicketId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Support ticket claimed successfully",
                supportTicketApiMapper.toAdminResponse(claimedTicket)
        ));
    }

    @PostMapping("/{supportTicketId}/customer-conversation")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> openCustomerConversationForReply(
            @PathVariable UUID supportTicketId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer conversation opened successfully",
                chatConversationApiMapper.toConversationUserResponse(
                        supportTicketPortIn.openCustomerConversationForReply(supportTicketId)
                )
        ));
    }

    @GetMapping("/{supportTicketId}/customer-conversation")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> getActiveCustomerConversation(
            @PathVariable UUID supportTicketId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Active customer conversation fetched successfully",
                chatConversationApiMapper.toConversationUserResponse(
                        supportTicketPortIn.getActiveCustomerConversation(supportTicketId)
                )
        ));
    }
}
