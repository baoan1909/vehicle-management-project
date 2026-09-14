package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiRunPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiModelWarningPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.ai.model.AiFunctionCall;
import com.ban.vehicle_management.domain.ai.model.AiFunctionResponse;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.domain.ai.model.AiProviderErrorDetail;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.domain.ai.policy.AiToolDefinition;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningSeverity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AssistantOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssistantOrchestrator.class);
    private static final int JOB_BATCH_SIZE = 4;
    private final String workerId = "assistant-worker-" + UUID.randomUUID();

    private final AiAssistantProperties properties;
    private final AssistantJobPortOut assistantJobPortOut;
    private final ChatConversationPortOut chatPortOut;
    private final AiModelRouter modelRouter;
    private final AiModelPolicyService modelPolicyService;
    private final Map<AiProvider, AiProviderPortOut> providerPorts;
    private final AiRunPortOut aiRunPortOut;
    private final AiModelWarningPortOut aiModelWarningPortOut;
    private final AiToolCallPortOut aiToolCallPortOut;
    private final AiToolExecutionService toolExecutionService;
    private final AiToolRegistry toolRegistry;
    private final ChatRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final ChatRealtimeEventMapper realtimeEventMapper;
    private final PiiRedactionService redactionService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ChatMessagePolicy messagePolicy = new ChatMessagePolicy();

    public AssistantOrchestrator(
            AiAssistantProperties properties,
            AssistantJobPortOut assistantJobPortOut,
            ChatConversationPortOut chatPortOut,
            AiModelRouter modelRouter,
            AiModelPolicyService modelPolicyService,
            List<AiProviderPortOut> aiProviderPorts,
            AiRunPortOut aiRunPortOut,
            AiModelWarningPortOut aiModelWarningPortOut,
            AiToolCallPortOut aiToolCallPortOut,
            AiToolExecutionService toolExecutionService,
            AiToolRegistry toolRegistry,
            ChatRealtimeEventPublisherPortOut realtimeEventPublisher,
            ChatRealtimeEventMapper realtimeEventMapper,
            PiiRedactionService redactionService,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.properties = properties;
        this.assistantJobPortOut = assistantJobPortOut;
        this.chatPortOut = chatPortOut;
        this.modelRouter = modelRouter;
        this.modelPolicyService = modelPolicyService;
        this.providerPorts = new EnumMap<>(AiProvider.class);
        for (AiProviderPortOut providerPort : aiProviderPorts) {
            this.providerPorts.put(providerPort.provider(), providerPort);
        }
        this.aiRunPortOut = aiRunPortOut;
        this.aiModelWarningPortOut = aiModelWarningPortOut;
        this.aiToolCallPortOut = aiToolCallPortOut;
        this.toolExecutionService = toolExecutionService;
        this.toolRegistry = toolRegistry;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.realtimeEventMapper = realtimeEventMapper;
        this.redactionService = redactionService;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelayString = "${app.ai.assistant-job-fixed-delay-ms:2500}", initialDelayString = "${app.ai.assistant-job-initial-delay-ms:5000}")
    public void processDueJobs() {
        if (!properties.isProviderCallAllowed()) {
            return;
        }
        Instant now = Instant.now();
        List<AssistantJob> dueJobs = assistantJobPortOut.claimDueJobs(now, now.plus(lockDuration()), workerId, JOB_BATCH_SIZE);
        dueJobs.forEach(this::processJobSafely);
    }

    private void processJobSafely(AssistantJob job) {
        try {
            processJob(job);
        } catch (RuntimeException exception) {
            LOGGER.warn("AI assistant job failed jobId={} conversationId={} inputMessageId={} error={}",
                    job.getJobId(), job.getConversationId(), job.getInputMessageId(),
                    exception.getClass().getSimpleName(), exception);
            transactionTemplate.executeWithoutResult(status -> failOrRetry(job, "UNEXPECTED_ERROR", true));
        }
    }

    public void processJob(AssistantJob job) {
        JobContext context = transactionTemplate.execute(status -> loadJobContext(job));
        if (context == null || context.terminal()) {
            return;
        }

        PiiRedactionService.RedactionResult inputRedaction = redactionService.redact(context.inputMessage().getContent());
        if (inputRedaction.sensitivePayment()) {
            transactionTemplate.executeWithoutResult(status -> {
                saveAssistantMessage(context.conversation(), job, "Vì lý do bảo mật, trợ lý không xử lý nội dung chứa dữ liệu thanh toán hoặc thông tin nhạy cảm. Vui lòng xóa dữ liệu nhạy cảm và gửi lại.");
                failOrRetry(job, "SENSITIVE_DATA_BLOCKED", false);
            });
            return;
        }

        List<AiModelConfiguration> candidates = modelRouter.resolveCandidates(
                AiUseCase.SUPPORT_CHAT,
                context.conversation().getConversationId(),
                context.inputMessage().getSenderAccountId(),
                1
        );
        String lastFailureCode = null;
        boolean lastFailureRetryable = false;
        for (AiModelConfiguration configuration : candidates) {
            AiProviderPortOut providerPort = providerPorts.get(resolveProvider(configuration));
            if (providerPort == null) {
                lastFailureCode = "PROVIDER_NOT_CONFIGURED";
                lastFailureRetryable = false;
                continue;
            }
            AiRun run = transactionTemplate.execute(status -> createRunningRun(context.conversation(), context.inputMessage(), configuration));
            Instant startedAt = Instant.now();
            AiResponse response;
            try {
                response = providerPort.generate(buildAiRequest(context.conversation()), configuration);
            } catch (RuntimeException exception) {
                lastFailureCode = "PROVIDER_EXCEPTION";
                lastFailureRetryable = true;
                String failureCode = lastFailureCode;
                transactionTemplate.executeWithoutResult(status -> markRunFailed(run, failureCode));
                continue;
            }
            run.setLatencyMs(Duration.between(startedAt, Instant.now()).toMillis());
            run.setInputTokens(response.inputTokens());
            run.setOutputTokens(response.outputTokens());
            if (response.success()) {
                if (response.functionCall() != null) {
                    if (handleFunctionCall(context, job, run, configuration, response.functionCall())) {
                        return;
                    }
                    lastFailureCode = "INVALID_FUNCTION_CALL";
                    lastFailureRetryable = false;
                    String failureCode = lastFailureCode;
                    transactionTemplate.executeWithoutResult(status -> markRunFailed(run, failureCode));
                    break;
                }
                String answer = extractAssistantText(response.text());
                if (answer == null || answer.isBlank()) {
                    lastFailureCode = "INVALID_RESPONSE_SCHEMA";
                    lastFailureRetryable = true;
                    String failureCode = lastFailureCode;
                    transactionTemplate.executeWithoutResult(status -> markRunFailed(run, failureCode));
                    if (modelPolicyService.canFallbackFor(lastFailureCode)) {
                        continue;
                    }
                    break;
                }
                transactionTemplate.executeWithoutResult(status -> storeSuccessfulResponse(context.conversation(), job, run, answer));
                return;
            }

            lastFailureCode = response.failureCode();
            lastFailureRetryable = response.retryable();
            transactionTemplate.executeWithoutResult(status -> {
                markRunFailed(run, response);
                if ("MODEL_NOT_FOUND".equals(response.failureCode())) {
                    openModelWarning(configuration, response);
                }
            });
            if (!modelPolicyService.canFallbackFor(lastFailureCode)) {
                break;
            }
        }

        String failureCode = lastFailureCode == null ? "AI_PROVIDER_FAILED" : lastFailureCode;
        boolean retryable = lastFailureRetryable;
        transactionTemplate.executeWithoutResult(status -> failOrRetry(job, failureCode, retryable));
    }

    private JobContext loadJobContext(AssistantJob job) {
        if (aiRunPortOut.existsSuccessfulRunForInputMessage(job.getInputMessageId())) {
            completeJob(job);
            return JobContext.terminalContext();
        }
        ChatConversation conversation = chatPortOut.findConversationById(job.getConversationId()).orElse(null);
        ChatMessage inputMessage = chatPortOut.findMessageById(job.getInputMessageId()).orElse(null);
        if (conversation == null || inputMessage == null) {
            failOrRetry(job, "SOURCE_NOT_FOUND", false);
            return JobContext.terminalContext();
        }
        if (inputMessage.getMessageType() != ChatMessageType.TEXT || inputMessage.isDeleted()) {
            completeJob(job);
            return JobContext.terminalContext();
        }
        return new JobContext(conversation, inputMessage, false);
    }

    private AiRequest buildAiRequest(ChatConversation conversation) {
        List<ChatMessage> history = chatPortOut.findMessageHistory(
                conversation.getConversationId(),
                null,
                Math.max(1, properties.getRecentMessageLimit())
        );
        List<AiRequestMessage> messages = new ArrayList<>();
        List<ChatMessage> chronological = new ArrayList<>(history);
        Collections.reverse(chronological);
        for (ChatMessage message : chronological) {
            if (message.isDeleted() || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String role = message.getSenderAccountId() == null ? "model" : "user";
            String content = redactionService.redact(message.getContent()).value();
            messages.add(new AiRequestMessage(role, content));
        }
        return new AiRequest(
                systemInstruction(),
                messages,
                true,
                toolRegistry.all().stream().map(AiToolDefinition::declaration).toList(),
                List.of()
        );
    }

    private AiRequest buildAiRequestWithFunctionResponse(ChatConversation conversation, AiFunctionResponse functionResponse) {
        List<AiRequestMessage> messages = new ArrayList<>(buildAiRequest(conversation).messages());
        messages.add(new AiRequestMessage("user", "Hãy tạo câu trả lời cuối cùng ngắn gọn dựa trên kết quả của công cụ. Nói rõ nếu không có đủ nguồn thông tin."));
        return new AiRequest(systemInstruction(), messages, true, List.of(), List.of(functionResponse));
    }

    private AiProvider resolveProvider(AiModelConfiguration configuration) {
        return configuration.getProvider() == null ? AiProvider.GEMINI : configuration.getProvider();
    }

    private String systemInstruction() {
        return """
                You are CoParking support assistant. Answer in Vietnamese.
                Use only the provided conversation context. Do not ask for secrets, tokens, payment card data, or full identity numbers.
                Do not claim a support ticket was created unless the backend has confirmed it.
                Return strict JSON with this shape: {"responseText":"..."}.
                If the customer needs an official support ticket, ask them to use the create-ticket confirmation action in the widget.
                """;
    }

    private String extractAssistantText(String text) {
        if (text == null) {
            return null;
        }
        String candidate = text.trim();
        if (candidate.startsWith("```")) {
            candidate = candidate.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        if (candidate.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(candidate);
                JsonNode responseText = root.get("responseText");
                return responseText != null && responseText.isTextual() ? sanitizeAssistantOutput(responseText.asText()) : null;
            } catch (Exception exception) {
                return null;
            }
        }
        return sanitizeAssistantOutput(candidate);
    }

    private String sanitizeAssistantOutput(String value) {
        String sanitized = redactionService.redact(value == null ? "" : value).value()
                .replace('<', '[')
                .replace('>', ']')
                .trim();
        if (sanitized.length() > ChatMessagePolicy.MAX_TEXT_CONTENT_LENGTH) {
            return sanitized.substring(0, ChatMessagePolicy.MAX_TEXT_CONTENT_LENGTH);
        }
        return sanitized;
    }

    private AiRun createRunningRun(ChatConversation conversation, ChatMessage inputMessage, AiModelConfiguration configuration) {
        AiRun run = new AiRun();
        run.setRunId(UUID.randomUUID());
        run.setConversationId(conversation.getConversationId());
        run.setInputMessageId(inputMessage.getMessageId());
        run.setConfigurationId(configuration.getConfigurationId());
        run.setProvider(resolveProvider(configuration));
        run.setModelId(configuration.getModelId());
        run.setPromptVersion(properties.getPromptVersion());
        run.setRolloutVersion(1);
        run.setAttemptNumber(1);
        run.setStatus(AiRunStatus.RUNNING);
        run.setCreatedAt(Instant.now());
        return aiRunPortOut.save(run);
    }

    private boolean handleFunctionCall(
            JobContext context,
            AssistantJob job,
            AiRun run,
            AiModelConfiguration configuration,
            AiFunctionCall functionCall
    ) {
        if (functionCall.malformed()) {
            transactionTemplate.executeWithoutResult(status -> markRunFailed(run, functionCall.failureCode()));
            return false;
        }
        JsonNode arguments;
        try {
            arguments = objectMapper.readTree(functionCall.argumentsJson());
        } catch (Exception exception) {
            transactionTemplate.executeWithoutResult(status -> markRunFailed(run, "MALFORMED_FUNCTION_CALL"));
            return false;
        }

        AiToolDefinition definition;
        try {
            definition = toolRegistry.require(functionCall.name());
            definition.validate(arguments);
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(status -> {
                AiToolCall denied = newToolCall(run, context, functionCall.name(), AiToolType.READ_ONLY, functionCall.argumentsJson());
                denied.setStatus(AiToolCallStatus.FAILED);
                denied.setFailureCode(exception.getClass().getSimpleName());
                aiToolCallPortOut.save(denied);
            });
            return false;
        }

        if (definition.requiresConfirmation()) {
            transactionTemplate.executeWithoutResult(status -> createPendingActionCard(context, job, run, definition, functionCall.argumentsJson()));
            return true;
        }

        AiToolExecutionService.ToolExecutionResult toolResult;
        AiToolCall toolCall = transactionTemplate.execute(status -> {
            AiToolCall requested = newToolCall(run, context, definition.name(), definition.toolType(), functionCall.argumentsJson());
            requested.setStatus(AiToolCallStatus.EXECUTING);
            return aiToolCallPortOut.save(requested);
        });
        try {
            toolResult = toolExecutionService.executeFromAssistantWorker(definition.name(), arguments);
            AiToolCall savedToolCall = toolCall;
            transactionTemplate.executeWithoutResult(status -> {
                savedToolCall.setStatus(AiToolCallStatus.SUCCEEDED);
                savedToolCall.setExecutedAt(Instant.now());
                savedToolCall.setResponsePayloadRedacted(toolResult.responseJson());
                aiToolCallPortOut.save(savedToolCall);
            });
        } catch (RuntimeException exception) {
            AiToolCall failedToolCall = toolCall;
            transactionTemplate.executeWithoutResult(status -> {
                failedToolCall.setStatus(AiToolCallStatus.FAILED);
                failedToolCall.setFailureCode(exception.getClass().getSimpleName());
                aiToolCallPortOut.save(failedToolCall);
            });
            return false;
        }

        AiProviderPortOut providerPort = providerPorts.get(resolveProvider(configuration));
        AiResponse finalResponse = providerPort.generate(
                buildAiRequestWithFunctionResponse(context.conversation(), new AiFunctionResponse(definition.name(), toolResult.responseJson())),
                configuration
        );
        String answer = finalResponse.success() ? extractAssistantText(finalResponse.text()) : null;
        if (answer == null || answer.isBlank()) {
            answer = summarizeToolResponse(toolResult.responseJson());
        }
        String finalAnswer = answer;
        transactionTemplate.executeWithoutResult(status -> storeSuccessfulResponse(context.conversation(), job, run, finalAnswer));
        return true;
    }

    private void createPendingActionCard(
            JobContext context,
            AssistantJob job,
            AiRun run,
            AiToolDefinition definition,
            String argumentsJson
    ) {
        AiToolCall toolCall = newToolCall(run, context, definition.name(), definition.toolType(), argumentsJson);
        toolCall.setStatus(AiToolCallStatus.AWAITING_CONFIRMATION);
        toolCall.setExpiresAt(Instant.now().plus(definition.timeout()));
        toolCall = aiToolCallPortOut.save(toolCall);
        ChatMessage actionCard = saveActionCardMessage(context.conversation(), job, toolCall, definition);
        run.setOutputMessageId(actionCard.getMessageId());
        run.setStatus(AiRunStatus.SUCCEEDED);
        aiRunPortOut.save(run);
        job.setStatus(AssistantJobStatus.WAITING_CONFIRMATION);
        clearLock(job);
        assistantJobPortOut.save(job);
    }

    private AiToolCall newToolCall(AiRun run, JobContext context, String toolName, AiToolType toolType, String argumentsJson) {
        AiToolCall toolCall = new AiToolCall();
        toolCall.setToolCallId(UUID.randomUUID());
        toolCall.setRunId(run.getRunId());
        toolCall.setConversationId(context.conversation().getConversationId());
        toolCall.setInputMessageId(context.inputMessage().getMessageId());
        toolCall.setRequestedBy(context.inputMessage().getSenderAccountId());
        toolCall.setToolName(toolName);
        toolCall.setToolType(toolType);
        toolCall.setRequestPayloadRedacted(toObjectJson(Map.of("toolName", toolName)));
        toolCall.setArgumentPayloadRedacted(argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson);
        toolCall.setResponsePayloadRedacted("{}");
        toolCall.setStatus(AiToolCallStatus.REQUESTED);
        toolCall.setIdempotencyKey("ai-tool:" + context.inputMessage().getMessageId() + ":" + toolName);
        toolCall.setCreatedAt(Instant.now());
        toolCall.setVersion(0);
        return toolCall;
    }

    private ChatMessage saveActionCardMessage(
            ChatConversation conversation,
            AssistantJob job,
            AiToolCall toolCall,
            AiToolDefinition definition
    ) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID());
        message.setConversationId(conversation.getConversationId());
        message.setSenderAccountId(null);
        message.setReplyToMessageId(job.getInputMessageId());
        message.setRelatedSchema("ai");
        message.setRelatedTable("ai_tool_calls");
        message.setRelatedId(toolCall.getToolCallId());
        message.setContent(toObjectJson(Map.of(
                "toolCallId", toolCall.getToolCallId().toString(),
                "toolName", toolCall.getToolName(),
                "title", actionTitle(definition.name()),
                "description", actionDescription(definition.name()),
                "expiresAt", toolCall.getExpiresAt() == null ? "" : toolCall.getExpiresAt().toString()
        )));
        messagePolicy.initializeActionCard(message);
        ChatMessage savedMessage = chatPortOut.saveMessage(message);
        conversation.setLastMessageId(savedMessage.getMessageId());
        conversation.setLastMessageAt(savedMessage.getCreatedAt() == null ? Instant.now() : savedMessage.getCreatedAt());
        chatPortOut.saveConversation(conversation);
        TransactionalEvents.runAfterCommit(() -> realtimeEventPublisher.publish(
                realtimeEventMapper.toRealtimeEvent(savedMessage, Instant.now())
        ));
        return savedMessage;
    }

    private String actionTitle(String toolName) {
        return switch (toolName) {
            case "create_support_ticket" -> "Xác nhận tạo phiếu hỗ trợ";
            default -> "Xác nhận hành động";
        };
    }

    private String actionDescription(String toolName) {
        return switch (toolName) {
            case "create_support_ticket" -> "Hệ thống sẽ tạo phiếu hỗ trợ với nội dung đã hiển thị sau khi bạn xác nhận.";
            default -> "Hệ thống chỉ thực hiện hành động sau khi bạn xác nhận.";
        };
    }

    private String summarizeToolResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode responseText = root.get("responseText");
            if (responseText != null && responseText.isTextual()) {
                return sanitizeAssistantOutput(responseText.asText());
            }
        } catch (Exception ignored) {
            return "Đã xử lý yêu cầu.";
        }
        return "Đã xử lý yêu cầu.";
    }

    private String toObjectJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private void storeSuccessfulResponse(ChatConversation conversation, AssistantJob job, AiRun run, String answer) {
        if (aiRunPortOut.existsSuccessfulRunForInputMessage(job.getInputMessageId())) {
            completeJob(job);
            return;
        }
        ChatMessage output = saveAssistantMessage(conversation, job, answer);
        run.setOutputMessageId(output.getMessageId());
        run.setStatus(AiRunStatus.SUCCEEDED);
        aiRunPortOut.save(run);
        completeJob(job);
    }

    private void markRunFailed(AiRun run, String failureCode) {
        run.setStatus(AiRunStatus.FAILED);
        run.setFailureCode(failureCode);
        aiRunPortOut.save(run);
    }

    private void markRunFailed(AiRun run, AiResponse response) {
        run.setStatus(AiRunStatus.FAILED);
        run.setFailureCode(response.failureCode());
        run.setFailureRetryable(response.retryable());
        AiProviderErrorDetail detail = response.providerErrorDetail();
        if (detail != null) {
            run.setProviderStatus(detail.providerStatus());
            run.setProviderErrorCode(detail.providerErrorCode());
            run.setProviderErrorMessageRedacted(detail.providerErrorMessageRedacted());
            run.setFieldViolationsRedacted(detail.fieldViolationsRedacted());
        }
        aiRunPortOut.save(run);
    }

    private void openModelWarning(AiModelConfiguration configuration, AiResponse response) {
        if (configuration == null) {
            return;
        }
        boolean exists = aiModelWarningPortOut.findOpen(
                resolveProvider(configuration).name(),
                configuration.getModelId(),
                configuration.getConfigurationId(),
                "MODEL_NOT_FOUND"
        ).isPresent();
        if (exists) {
            return;
        }
        AiModelWarning warning = new AiModelWarning();
        warning.setWarningId(UUID.randomUUID());
        warning.setProvider(resolveProvider(configuration));
        warning.setModelId(configuration.getModelId());
        warning.setConfigurationId(configuration.getConfigurationId());
        warning.setWarningCode("MODEL_NOT_FOUND");
        warning.setSeverity(AiModelWarningSeverity.CRITICAL);
        warning.setStatus(AiModelWarningStatus.OPEN);
        warning.setDetail(response.providerErrorDetail() == null
                ? "Configured Gemini model was not found"
                : response.providerErrorDetail().providerErrorMessageRedacted());
        warning.setDetectedAt(Instant.now());
        warning.setCreatedAt(Instant.now());
        aiModelWarningPortOut.save(warning);
    }

    private ChatMessage saveAssistantMessage(ChatConversation conversation, AssistantJob job, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID());
        message.setConversationId(conversation.getConversationId());
        message.setSenderAccountId(null);
        message.setContent(content);
        message.setReplyToMessageId(job.getInputMessageId());
        message.setRelatedSchema("ai");
        message.setRelatedTable("assistant_jobs");
        message.setRelatedId(job.getJobId());
        messagePolicy.initializeAssistantText(message);
        ChatMessage savedMessage = chatPortOut.saveMessage(message);
        conversation.setLastMessageId(savedMessage.getMessageId());
        conversation.setLastMessageAt(savedMessage.getCreatedAt() == null ? Instant.now() : savedMessage.getCreatedAt());
        chatPortOut.saveConversation(conversation);
        TransactionalEvents.runAfterCommit(() -> realtimeEventPublisher.publish(
                realtimeEventMapper.toRealtimeEvent(savedMessage, Instant.now())
        ));
        return savedMessage;
    }

    private void completeJob(AssistantJob job) {
        job.setStatus(AssistantJobStatus.COMPLETED);
        job.setErrorCode(null);
        clearLock(job);
        assistantJobPortOut.save(job);
    }

    private void failOrRetry(AssistantJob job, String errorCode, boolean retryable) {
        int attempts = job.getAttemptCount() == null ? 1 : job.getAttemptCount();
        job.setErrorCode(errorCode);
        clearLock(job);
        if (retryable && attempts < Math.max(1, properties.getMaxAttempts())) {
            long multiplier = 1L << Math.min(5, Math.max(0, attempts - 1));
            job.setStatus(AssistantJobStatus.RETRYING);
            job.setNextAttemptAt(Instant.now().plus(properties.getRetryInitialDelay().multipliedBy(multiplier)));
        } else {
            job.setStatus(AssistantJobStatus.FAILED);
        }
        assistantJobPortOut.save(job);
    }

    private void clearLock(AssistantJob job) {
        job.setLockedAt(null);
        job.setLockedBy(null);
        job.setLockExpiresAt(null);
    }

    private Duration lockDuration() {
        Duration requestTimeout = properties.getRequestTimeout() == null ? Duration.ofSeconds(20) : properties.getRequestTimeout();
        return requestTimeout.plusSeconds(90);
    }

    private record JobContext(ChatConversation conversation, ChatMessage inputMessage, boolean terminal) {

        private static JobContext terminalContext() {
            return new JobContext(null, null, true);
        }
    }
}
