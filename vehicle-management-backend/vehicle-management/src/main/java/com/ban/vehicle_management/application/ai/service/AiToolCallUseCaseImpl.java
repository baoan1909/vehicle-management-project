package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AiToolCallPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiToolCallUseCaseImpl implements AiToolCallPortIn {

    private final AiToolCallPortOut aiToolCallPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final AiToolExecutionService toolExecutionService;
    private final ChatConversationPortOut chatPortOut;
    private final ChatRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final ChatRealtimeEventMapper realtimeEventMapper;
    private final ObjectMapper objectMapper;
    private final ChatMessagePolicy messagePolicy = new ChatMessagePolicy();

    public AiToolCallUseCaseImpl(
            AiToolCallPortOut aiToolCallPortOut,
            CurrentAccountPortIn currentAccountPortIn,
            AiToolExecutionService toolExecutionService,
            ChatConversationPortOut chatPortOut,
            ChatRealtimeEventPublisherPortOut realtimeEventPublisher,
            ChatRealtimeEventMapper realtimeEventMapper,
            ObjectMapper objectMapper
    ) {
        this.aiToolCallPortOut = aiToolCallPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.toolExecutionService = toolExecutionService;
        this.chatPortOut = chatPortOut;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.realtimeEventMapper = realtimeEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AiToolCall getToolCall(UUID toolCallId) {
        AiToolCall toolCall = aiToolCallPortOut.findById(toolCallId)
                .orElseThrow(() -> new NotFoundException("AI tool call not found"));
        requireOwner(toolCall);
        return toolCall;
    }

    @Override
    @Transactional
    public AiToolCall confirm(UUID toolCallId) {
        AiToolCall toolCall = aiToolCallPortOut.findByIdForUpdate(toolCallId)
                .orElseThrow(() -> new NotFoundException("AI tool call not found"));
        requireOwner(toolCall);
        Instant now = Instant.now();
        if (toolCall.getToolType() != AiToolType.WRITE) {
            throw new BadRequestException("Only write tool calls require confirmation");
        }
        if (toolCall.getStatus() == AiToolCallStatus.SUCCEEDED || toolCall.getStatus() == AiToolCallStatus.EXECUTING) {
            return toolCall;
        }
        if (toolCall.getStatus() != AiToolCallStatus.AWAITING_CONFIRMATION) {
            throw new BadRequestException("AI tool call cannot be confirmed in current status");
        }
        if (toolCall.getExpiresAt() == null || !now.isBefore(toolCall.getExpiresAt())) {
            toolCall.setStatus(AiToolCallStatus.EXPIRED);
            toolCall.setFailureCode("CONFIRMATION_EXPIRED");
            return aiToolCallPortOut.save(toolCall);
        }

        toolCall.setStatus(AiToolCallStatus.EXECUTING);
        toolCall.setConfirmedAt(now);
        toolCall.setConfirmedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        toolCall = aiToolCallPortOut.save(toolCall);

        try {
            JsonNode frozenArguments = objectMapper.readTree(toolCall.getArgumentPayloadRedacted());
            AiToolExecutionService.ToolExecutionResult result = toolExecutionService.execute(
                    toolCall.getToolName(),
                    frozenArguments,
                    toolCall.getConversationId(),
                    toolCall.getIdempotencyKey()
            );
            toolCall.setResponsePayloadRedacted(result.responseJson());
            toolCall.setExecutedAt(Instant.now());
            toolCall.setStatus(AiToolCallStatus.SUCCEEDED);
            AiToolCall saved = aiToolCallPortOut.save(toolCall);
            publishToolResult(saved, "Đã thực hiện hành động bạn xác nhận.");
            return saved;
        } catch (RuntimeException exception) {
            toolCall.setStatus(AiToolCallStatus.FAILED);
            toolCall.setFailureCode(exception.getClass().getSimpleName());
            AiToolCall saved = aiToolCallPortOut.save(toolCall);
            publishToolResult(saved, "Hành động chưa thực hiện được. Vui lòng thử lại hoặc liên hệ nhân viên hỗ trợ.");
            return saved;
        } catch (Exception exception) {
            toolCall.setStatus(AiToolCallStatus.FAILED);
            toolCall.setFailureCode("INVALID_FROZEN_ARGUMENTS");
            return aiToolCallPortOut.save(toolCall);
        }
    }

    @Override
    @Transactional
    public AiToolCall deny(UUID toolCallId) {
        AiToolCall toolCall = aiToolCallPortOut.findByIdForUpdate(toolCallId)
                .orElseThrow(() -> new NotFoundException("AI tool call not found"));
        requireOwner(toolCall);
        if (toolCall.getStatus() != AiToolCallStatus.AWAITING_CONFIRMATION) {
            return toolCall;
        }
        toolCall.setStatus(AiToolCallStatus.DENIED);
        toolCall.setConfirmedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        toolCall.setConfirmedAt(Instant.now());
        AiToolCall saved = aiToolCallPortOut.save(toolCall);
        publishToolResult(saved, "Bạn đã hủy hành động này. Không có thay đổi nào được thực hiện.");
        return saved;
    }

    private void requireOwner(AiToolCall toolCall) {
        UUID currentAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        if (!Objects.equals(toolCall.getRequestedBy(), currentAccountId)) {
            throw new AccessDeniedException("Access is denied");
        }
    }

    private void publishToolResult(AiToolCall toolCall, String text) {
        if (toolCall.getConversationId() == null) {
            return;
        }
        ChatConversation conversation = chatPortOut.findConversationById(toolCall.getConversationId()).orElse(null);
        if (conversation == null) {
            return;
        }
        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID());
        message.setConversationId(toolCall.getConversationId());
        message.setSenderAccountId(null);
        message.setReplyToMessageId(toolCall.getInputMessageId());
        message.setRelatedSchema("ai");
        message.setRelatedTable("ai_tool_calls");
        message.setRelatedId(toolCall.getToolCallId());
        message.setContent(text);
        messagePolicy.initializeToolResult(message);
        ChatMessage saved = chatPortOut.saveMessage(message);
        conversation.setLastMessageId(saved.getMessageId());
        conversation.setLastMessageAt(saved.getCreatedAt() == null ? Instant.now() : saved.getCreatedAt());
        chatPortOut.saveConversation(conversation);
        TransactionalEvents.runAfterCommit(() -> realtimeEventPublisher.publish(
                realtimeEventMapper.toRealtimeEvent(saved, Instant.now())
        ));
    }
}
