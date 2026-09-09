package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatConversationApiMapper;
import com.ban.vehicle_management.application.operations.chatconversation.model.ChatAttachmentReadUrl;
import com.ban.vehicle_management.application.operations.chatconversation.port.in.ChatConversationPortIn;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatInboxItem;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request.AddChatConversationMemberRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request.CreateCustomerSupportConversationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request.CreateInternalDirectConversationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.request.CreateInternalGroupConversationRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatConversationUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatInboxItemUserResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.request.MarkConversationReadRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.request.SendChatMessageRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatAttachmentReadUrlResponse;
import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatMessageUserResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/operations/chat")
public class ChatConversationController {

    private final ChatConversationPortIn chatConversationPortIn;
    private final ChatConversationApiMapper chatConversationApiMapper;

    public ChatConversationController(
            ChatConversationPortIn chatConversationPortIn,
            ChatConversationApiMapper chatConversationApiMapper
    ) {
        this.chatConversationPortIn = chatConversationPortIn;
        this.chatConversationApiMapper = chatConversationApiMapper;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ChatInboxItemUserResponse>>> getInbox() {
        List<ChatInboxItem> inboxItems = chatConversationPortIn.getInbox();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched chat inbox successfully",
                chatConversationApiMapper.toInboxItemUserResponses(inboxItems)
        ));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> getConversation(
            @PathVariable UUID conversationId
    ) {
        ChatConversation conversation = chatConversationPortIn.getConversation(conversationId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched chat conversation successfully",
                chatConversationApiMapper.toConversationUserResponse(conversation)
        ));
    }

    @PostMapping("/conversations/internal/direct")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> createInternalDirectConversation(
            @RequestBody CreateInternalDirectConversationRequest request
    ) {
        ChatConversation conversation = chatConversationPortIn.createOrGetInternalDirectConversation(
                request.targetAccountId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Internal direct conversation created or fetched successfully",
                chatConversationApiMapper.toConversationUserResponse(conversation)
        ));
    }

    @PostMapping("/conversations/internal/groups")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> createInternalGroupConversation(
            @RequestBody CreateInternalGroupConversationRequest request
    ) {
        ChatConversation conversation = chatConversationPortIn.createInternalGroupConversation(
                request.title(),
                request.memberAccountIds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Internal group conversation created successfully",
                chatConversationApiMapper.toConversationUserResponse(conversation)
        ));
    }

    @PostMapping("/conversations/customer-support")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> createCustomerSupportConversation(
            @RequestBody CreateCustomerSupportConversationRequest request
    ) {
        ChatConversation conversation = chatConversationPortIn.createOrGetCustomerSupportConversation(
                request.customerId(),
                request.title()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Customer support conversation created or fetched successfully",
                chatConversationApiMapper.toConversationUserResponse(conversation)
        ));
    }

    @PostMapping("/conversations/{conversationId}/members")
    public ResponseEntity<ApiResponse<ChatConversationUserResponse>> addMember(
            @PathVariable UUID conversationId,
            @RequestBody AddChatConversationMemberRequest request
    ) {
        ChatConversation conversation = chatConversationPortIn.addMember(conversationId, request.accountId());
        return ResponseEntity.ok(ApiResponse.ok(
                "Chat conversation member added successfully",
                chatConversationApiMapper.toConversationUserResponse(conversation)
        ));
    }

    @DeleteMapping("/conversations/{conversationId}/members/{accountId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID conversationId,
            @PathVariable UUID accountId
    ) {
        chatConversationPortIn.removeMember(conversationId, accountId);
        return ResponseEntity.ok(ApiResponse.ok("Chat conversation member removed successfully"));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageUserResponse>>> getMessageHistory(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant beforeCreatedAt,
            @RequestParam(defaultValue = "30") int limit
    ) {
        List<ChatMessage> messages = chatConversationPortIn.getMessageHistory(conversationId, beforeCreatedAt, limit);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched chat message history successfully",
                chatConversationApiMapper.toMessageUserResponses(messages)
        ));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageUserResponse>> sendTextMessage(
            @PathVariable UUID conversationId,
            @RequestBody SendChatMessageRequest request
    ) {
        ChatMessage message = chatConversationPortIn.sendTextMessage(
                conversationId,
                request.content(),
                request.replyToMessageId(),
                request.contextTicketId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Chat message sent successfully",
                chatConversationApiMapper.toMessageUserResponse(message)
        ));
    }

    @PostMapping(
            value = "/conversations/{conversationId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ChatMessageUserResponse>> sendImageMessage(
            @PathVariable UUID conversationId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) UUID contextTicketId
    ) {
        ChatMessage message = chatConversationPortIn.sendImageMessage(conversationId, content, files, contextTicketId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Chat image message sent successfully",
                chatConversationApiMapper.toMessageUserResponse(message)
        ));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable UUID messageId) {
        chatConversationPortIn.deleteMessage(messageId);
        return ResponseEntity.ok(ApiResponse.ok("Chat message deleted successfully"));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID conversationId,
            @RequestBody(required = false) MarkConversationReadRequest request
    ) {
        chatConversationPortIn.markRead(conversationId, request == null ? null : request.messageId());
        return ResponseEntity.ok(ApiResponse.ok("Chat conversation marked as read successfully"));
    }

    @GetMapping("/attachments/{attachmentId}/read-url")
    public ResponseEntity<ApiResponse<ChatAttachmentReadUrlResponse>> createAttachmentReadUrl(
            @PathVariable UUID attachmentId
    ) {
        ChatAttachmentReadUrl readUrl = chatConversationPortIn.createAttachmentReadUrl(attachmentId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Created chat attachment read URL successfully",
                chatConversationApiMapper.toReadUrlResponse(readUrl)
        ));
    }
}
