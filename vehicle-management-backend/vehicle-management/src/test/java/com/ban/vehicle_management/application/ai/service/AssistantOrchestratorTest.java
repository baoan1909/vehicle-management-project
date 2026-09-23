package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiMessageCitationPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiModelWarningPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiRunPortOut;
import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiFunctionCall;
import com.ban.vehicle_management.domain.ai.model.AiProviderErrorDetail;
import com.ban.vehicle_management.domain.ai.model.AiRequest;
import com.ban.vehicle_management.domain.ai.model.AiResponse;
import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.infrastructure.security.assistant.AssistantActorScope;
import com.ban.vehicle_management.domain.ai.policy.AiToolRegistry;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AssistantJobStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

    @Mock
    private KnowledgeRetrievalService retrievalService;

    @Mock
    private KnowledgeRetrievalAuditPortOut retrievalAuditPortOut;

    @Mock
    private AiMessageCitationPortOut citationPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private KnowledgeAccessContextResolver accessContextResolver;

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
                accessContextResolver,
                retrievalService,
                new RetrievalProperties(),
                retrievalAuditPortOut,
                citationPortOut,
                currentAccountPortIn,
                new AssistantActorScope(),
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

    @Test
    void greetingAnswersDeterministicallyWithoutProviderOrEmbedding() {
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

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        org.mockito.Mockito.lenient().when(chatPortOut.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.never())
                .generate(any(AiRequest.class), any(AiModelConfiguration.class));
        org.mockito.Mockito.verify(retrievalService, org.mockito.Mockito.never())
                .search(any(), any(), any(), any(Integer.class));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatPortOut).saveMessage(messageCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                messageCaptor.getValue().getContent().contains("Xin chào"));
    }

    @Test
    void staticKnowledgeWithoutEvidenceHandsOffWithoutGeneration() {
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
        inputMessage.setContent("Quy trình đăng ký thẻ xe như thế nào?");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(accessContextResolver.resolveScopes()).thenReturn(List.of("PUBLIC"));
        when(retrievalService.search(any(KnowledgeRetrievalContext.class), any(String.class),
                eq(List.of("PUBLIC")), any(Integer.class)))
                .thenReturn(KnowledgeRetrievalResult.empty("INSUFFICIENT_EVIDENCE"));
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.never())
                .generate(any(AiRequest.class), any(AiModelConfiguration.class));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatPortOut).saveMessage(messageCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                messageCaptor.getValue().getContent().contains("phiếu hỗ trợ"));
    }

    @Test
    void staticKnowledgeUsesOneGroundedSchemaAndPersistsNormalizedCitation() {
        UUID conversationId = UUID.randomUUID();
        UUID inputMessageId = UUID.randomUUID();
        UUID senderAccountId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID indexVersionId = UUID.randomUUID();
        UUID retrievalAuditId = UUID.randomUUID();
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
        inputMessage.setContent("Quy trình xe vào và xe ra như thế nào?");

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");
        KnowledgeSearchResult evidence = new KnowledgeSearchResult(
                documentId, chunkId, "Quy trình xe vào và xe ra",
                "Xe vào làn IN, nhân viên quét thẻ và hệ thống nhận dạng biển số.",
                null, 1, "Luồng chính", new BigDecimal("0.91"));
        KnowledgeRetrievalResult retrieval = new KnowledgeRetrievalResult(
                null, indexVersionId, List.of(evidence), retrievalAuditId,
                new BigDecimal("0.90"), true, "quy trinh xe vao va xe ra", "test-v1");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(chatPortOut.findMessageHistory(conversationId, null, properties.getRecentMessageLimit()))
                .thenReturn(List.of(inputMessage));
        when(accessContextResolver.resolveScopes()).thenReturn(List.of("PUBLIC"));
        when(retrievalService.search(any(KnowledgeRetrievalContext.class), any(String.class),
                eq(List.of("PUBLIC")), any(Integer.class))).thenReturn(retrieval);
        when(modelRouter.resolveCandidates(AiUseCase.SUPPORT_CHAT, conversationId, senderAccountId, 1))
                .thenReturn(List.of(configuration));
        when(providerPort.generate(any(AiRequest.class), eq(configuration))).thenReturn(AiResponse.success(
                "{\"responseText\":\"Xe vào làn IN và nhân viên quét thẻ.\",\"citations\":[\"[c1]\"]}",
                "{}", 20, 10));
        when(aiRunPortOut.save(any(AiRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(citationPortOut.saveAll(any(UUID.class), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        ArgumentCaptor<AiRequest> requestCaptor = ArgumentCaptor.forClass(AiRequest.class);
        verify(providerPort).generate(requestCaptor.capture(), eq(configuration));
        String systemInstruction = requestCaptor.getValue().systemInstruction();
        assertTrue(systemInstruction.contains("\"citations\":[\"C1\"]"));
        assertEquals(1, systemInstruction.split("Return strict JSON", -1).length - 1);
        assertTrue(requestCaptor.getValue().responseJsonSchema().contains("\"citations\""));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.ban.vehicle_management.domain.ai.model.AiMessageCitation>> citationCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(citationPortOut).saveAll(any(UUID.class), citationCaptor.capture());
        assertEquals("C1", citationCaptor.getValue().getFirst().label());
        assertEquals(chunkId, citationCaptor.getValue().getFirst().chunkId());
    }

    @Test
    void schedulerUnexpectedErrorClosesRunningRunAndRetriesJob() {
        UUID inputMessageId = UUID.randomUUID();
        AssistantJob job = new AssistantJob();
        job.setJobId(UUID.randomUUID());
        job.setConversationId(UUID.randomUUID());
        job.setInputMessageId(inputMessageId);
        job.setAttemptCount(1);

        when(assistantJobPortOut.claimDueJobs(
                any(java.time.Instant.class), any(java.time.Instant.class), any(String.class), eq(4)))
                .thenReturn(List.of(job));
        when(chatPortOut.findConversationById(job.getConversationId()))
                .thenThrow(new IllegalStateException("simulated worker failure"));
        when(assistantJobPortOut.save(any(AssistantJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processDueJobs();

        verify(aiRunPortOut).failRunningForInputMessage(inputMessageId, "UNEXPECTED_ERROR", true);
        assertEquals(AssistantJobStatus.RETRYING, job.getStatus());
        assertEquals("UNEXPECTED_ERROR", job.getErrorCode());
    }

    @Test
    void personalDataShouldRejectModelTextWhenNoToolWasCalled() {
        ModelFlowContext context = arrangePersonalModelFlow();
        when(providerPort.generate(any(AiRequest.class), eq(context.configuration)))
                .thenReturn(AiResponse.success(
                        "{\"responseText\":\"Bạn có một phiếu đang mở.\"}", "{}", 5, 5));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.FAILED, context.job.getStatus());
        assertEquals("TOOL_CALL_REQUIRED", context.job.getErrorCode());
        org.mockito.Mockito.verify(chatPortOut, org.mockito.Mockito.never()).saveMessage(any(ChatMessage.class));
    }

    @Test
    void personalDataShouldExecuteToolAndReplayProtocolBeforeStoringAnswer() {
        ModelFlowContext context = arrangePersonalModelFlow();
        AiFunctionCall call = new AiFunctionCall(
                "list_my_support_tickets", "{}", false, null, "call-1", "signature-1");
        when(providerPort.generate(any(AiRequest.class), eq(context.configuration)))
                .thenReturn(AiResponse.functionCall(call, "{}", 5, 2, "STOP"))
                .thenReturn(AiResponse.success(
                        "{\"responseText\":\"Bạn chưa có phiếu hỗ trợ nào.\"}", "{}", 7, 6));
        when(aiToolCallPortOut.save(any(AiToolCall.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(toolExecutionService.executeFromAssistantWorker(
                eq("list_my_support_tickets"), any(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(new AiToolExecutionService.ToolExecutionResult(
                        "list_my_support_tickets", "{\"tickets\":[]}", false));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.COMPLETED, context.job.getStatus());
        ArgumentCaptor<AiRequest> requestCaptor = ArgumentCaptor.forClass(AiRequest.class);
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.times(2))
                .generate(requestCaptor.capture(), eq(context.configuration));
        AiRequest followUp = requestCaptor.getAllValues().get(1);
        assertEquals(1, followUp.precedingFunctionCalls().size());
        assertEquals(1, followUp.functionResponses().size());
        assertEquals("list_my_support_tickets", followUp.precedingFunctionCalls().getFirst().name());
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(messageCaptor.capture());
        assertEquals("Bạn chưa có phiếu hỗ trợ nào.", messageCaptor.getValue().getContent());
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
        inputMessage.setContent("Tôi muốn tạo phiếu hỗ trợ");

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

    private ModelFlowContext arrangePersonalModelFlow() {
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
        inputMessage.setContent("Xem phiếu của tôi");

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(chatPortOut.findMessageHistory(conversationId, null, properties.getRecentMessageLimit()))
                .thenReturn(List.of(inputMessage));
        when(modelRouter.resolveCandidates(AiUseCase.SUPPORT_CHAT, conversationId, senderAccountId, 1))
                .thenReturn(List.of(configuration));
        when(aiRunPortOut.save(any(AiRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(chatPortOut.saveMessage(any(ChatMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any(AssistantJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return new ModelFlowContext(job, configuration);
    }

    private record TestContext(AssistantJob job, AiModelConfiguration configuration, List<AiRun> savedRuns) {
    }

    private record ModelFlowContext(AssistantJob job, AiModelConfiguration configuration) {
    }
}
