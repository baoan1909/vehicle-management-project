package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelWarningPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiRunPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiProviderErrorDetail;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AssistantOrchestratorTest {

    @Mock
    private AssistantJobPortOut assistantJobPortOut;

    @Mock
    private ChatConversationPortOut chatPortOut;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private AiModelPolicyService modelPolicyService;

    @Mock
    private AiProviderPortOut providerPort;

    @Mock
    private AiRunPortOut aiRunPortOut;

    @Mock
    private AiModelWarningPortOut aiModelWarningPortOut;

    @Mock
    private AiToolCallPortOut aiToolCallPortOut;

    @Mock
    private AiToolExecutionService toolExecutionService;

    @Mock
    private ChatRealtimeEventPublisherPortOut realtimeEventPublisher;

    @Mock
    private ChatRealtimeEventMapper realtimeEventMapper;

    private AssistantOrchestrator orchestrator;
    private AiAssistantProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiAssistantProperties();
        properties.setAssistantEnabled(true);
        properties.setAllowUnpaidSupportData(true);
        properties.setMaxAttempts(3);
        properties.setRetryInitialDelay(Duration.ofSeconds(20));
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer<?>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(providerPort.provider()).thenReturn(AiProvider.GEMINI);
        orchestrator = new AssistantOrchestrator(
                properties,
                assistantJobPortOut,
                chatPortOut,
                modelRouter,
                modelPolicyService,
                List.of(providerPort),
                aiRunPortOut,
                aiModelWarningPortOut,
                aiToolCallPortOut,
                toolExecutionService,
                new AiToolRegistry(),
                realtimeEventPublisher,
                realtimeEventMapper,
                new PiiRedactionService(),
                new ObjectMapper(),
                transactionTemplate
        );
    }

    @Test
    void http400FailsJobWithoutRetryingOrCallingProviderAgain() {
        TestContext context = arrangeProviderFailure(AiResponse.failure(
                "HTTP_400",
                false,
                null,
                new AiProviderErrorDetail(400, "INVALID_ARGUMENT", "Invalid schema", "[]", false)
        ));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.FAILED, context.job.getStatus());
        assertEquals("HTTP_400", context.job.getErrorCode());
        verify(providerPort).generate(any(AiRequest.class), eq(context.configuration));
        AiRun failedRun = context.savedRuns.getLast();
        assertEquals(AiRunStatus.FAILED, failedRun.getStatus());
        assertEquals("HTTP_400", failedRun.getFailureCode());
        assertEquals(Boolean.FALSE, failedRun.getFailureRetryable());
        assertEquals(400, failedRun.getProviderStatus());
    }

    @Test
    void http429RetriesJobWhenAttemptsRemain() {
        TestContext context = arrangeProviderFailure(AiResponse.failure(
                "HTTP_429",
                true,
                null,
                new AiProviderErrorDetail(429, "RESOURCE_EXHAUSTED", "Too many requests", "[]", true)
        ));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.RETRYING, context.job.getStatus());
        assertEquals("HTTP_429", context.job.getErrorCode());
        verify(providerPort).generate(any(AiRequest.class), eq(context.configuration));
    }

    private TestContext arrangeProviderFailure(AiResponse response) {
        UUID conversationId = UUID.randomUUID();
        UUID inputMessageId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        AssistantJob job = new AssistantJob();
        job.setJobId(UUID.randomUUID());
        job.setConversationId(conversationId);
        job.setInputMessageId(inputMessageId);
        job.setAttemptCount(1);

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        ChatMessage inputMessage = new ChatMessage();
        inputMessage.setMessageId(inputMessageId);
        inputMessage.setConversationId(conversationId);
        inputMessage.setSenderAccountId(senderAccountId);
        inputMessage.setMessageType(ChatMessageType.TEXT);
        inputMessage.setContent("Xin chào");

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");
        List<AiRun> savedRuns = new ArrayList<>();

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(chatPortOut.findMessageHistory(conversationId, null, properties.getRecentMessageLimit())).thenReturn(List.of(inputMessage));
        when(modelRouter.resolveCandidates(AiUseCase.SUPPORT_CHAT, conversationId, senderAccountId, 1)).thenReturn(List.of(configuration));
        when(providerPort.generate(any(AiRequest.class), eq(configuration))).thenReturn(response);
        when(modelPolicyService.canFallbackFor(response.failureCode())).thenReturn(false);
        when(aiRunPortOut.save(any(AiRun.class))).thenAnswer(invocation -> {
            AiRun run = invocation.getArgument(0);
            AiRun snapshot = new AiRun();
            snapshot.setStatus(run.getStatus());
            snapshot.setFailureCode(run.getFailureCode());
            snapshot.setFailureRetryable(run.getFailureRetryable());
            snapshot.setProviderStatus(run.getProviderStatus());
            savedRuns.add(snapshot);
            return run;
        });
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        return new TestContext(job, configuration, savedRuns);
    }

    private record TestContext(AssistantJob job, AiModelConfiguration configuration, List<AiRun> savedRuns) {
    }
}
