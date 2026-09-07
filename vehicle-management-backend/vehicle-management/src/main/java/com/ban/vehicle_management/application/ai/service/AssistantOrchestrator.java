package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiRunPortOut;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiRequestMessage;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
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
                    job.getJobId(), job.getConversationId(), job.getInputMessageId(), exception.getClass().getSimpleName());
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
                saveAssistantMessage(context.conversation(), job, "Tro ly dang tam thoi chuyen yeu cau nay sang nhan vien vi noi dung co du lieu nhay cam.");
                failOrRetry(job, "SENSITIVE_DATA_BLOCKED", false);
            });
            return;
        }

        List<AiModelConfiguration> candidates = modelRouter.resolveCandidates(AiUseCase.SUPPORT_CHAT);
        String lastFailureCode = null;
        for (AiModelConfiguration configuration : candidates) {
            AiProviderPortOut providerPort = providerPorts.get(resolveProvider(configuration));
            if (providerPort == null) {
                lastFailureCode = "PROVIDER_NOT_CONFIGURED";
                continue;
            }
            AiRun run = transactionTemplate.execute(status -> createRunningRun(context.conversation(), context.inputMessage(), configuration));
            Instant startedAt = Instant.now();
            AiResponse response;
            try {
                response = providerPort.generate(buildAiRequest(context.conversation()), configuration);
            } catch (RuntimeException exception) {
                lastFailureCode = "PROVIDER_EXCEPTION";
                String failureCode = lastFailureCode;
                transactionTemplate.executeWithoutResult(status -> markRunFailed(run, failureCode));
                continue;
            }
            run.setLatencyMs(Duration.between(startedAt, Instant.now()).toMillis());
            run.setInputTokens(response.inputTokens());
            run.setOutputTokens(response.outputTokens());
            if (response.success()) {
                String answer = extractAssistantText(response.text());
                if (answer == null || answer.isBlank()) {
                    lastFailureCode = "INVALID_RESPONSE_SCHEMA";
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
            String failureCode = lastFailureCode;
            transactionTemplate.executeWithoutResult(status -> markRunFailed(run, failureCode));
            if (!response.retryable() && !modelPolicyService.canFallbackFor(lastFailureCode)) {
                break;
            }
        }

        String failureCode = lastFailureCode == null ? "AI_PROVIDER_FAILED" : lastFailureCode;
        transactionTemplate.executeWithoutResult(status -> failOrRetry(job, failureCode, true));
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
        return new AiRequest(systemInstruction(), messages, true);
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
        run.setProvider(resolveProvider(configuration));
        run.setModelId(configuration.getModelId());
        run.setPromptVersion(properties.getPromptVersion());
        run.setStatus(AiRunStatus.RUNNING);
        run.setCreatedAt(Instant.now());
        return aiRunPortOut.save(run);
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
        messagePolicy.initializeSystem(message);
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
