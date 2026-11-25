package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.time.Instant;
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

    @org.junit.jupiter.api.AfterEach
    void closeWorker() { if (orchestrator != null) orchestrator.close(); }

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
        assertTrue(context.job.getNextAttemptAt().isAfter(java.time.Instant.now().plusSeconds(15)));
        verify(providerPort).generate(any(AiRequest.class), eq(context.configuration));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"TIMEOUT,false", "TIMEOUT,true", "GEMINI_REQUEST_BUDGET_EXHAUSTED,false", "GEMINI_REQUEST_BUDGET_EXHAUSTED,true"})
    void timeoutKeepsRealCauseAfterSkippedCandidatesAndRetriesWithinOneSecond(String code, boolean expired) {
        TestContext context = arrangeProviderFailure(AiResponse.failure(code, true, null));
        org.mockito.Mockito.reset(providerPort);
        var first = new com.ban.vehicle_management.domain.ai.model.AiProviderAttempt(context.configuration, "primary", java.time.Instant.now().plusSeconds(12));
        var skipped = new com.ban.vehicle_management.domain.ai.model.AiProviderAttempt(context.configuration, "backup-1", expired ? java.time.Instant.EPOCH : first.deadline());
        when(providerPort.generationAttempts(any())).thenReturn(List.of(first, skipped));
        when(providerPort.generateAttempt(any(), eq(first))).thenReturn(AiResponse.failure(code, true, null));
        if (!expired) when(providerPort.generateAttempt(any(), eq(skipped))).thenReturn(AiResponse.failure("GEMINI_CANDIDATE_UNAVAILABLE", true, null));
        when(modelPolicyService.canFallbackFor(code)).thenReturn(true);
        java.time.Instant started = java.time.Instant.now();
        orchestrator.processJob(context.job);
        assertEquals(AssistantJobStatus.RETRYING, context.job.getStatus());
        assertEquals(code, context.job.getErrorCode());
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.times(expired ? 0 : 1)).generateAttempt(any(), eq(skipped));
        assertEquals(expired ? 2 : 4, context.savedRuns.size(), "Expired candidates must not create an extra audit run");
        assertTrue(context.job.getNextAttemptAt().isBefore(started.plusSeconds(3)));
        assertTrue(context.job.getNextAttemptAt().isAfter(started));
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
    void embeddingFailureStopsWithExplicitServiceErrorWithoutGeneration() {
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
                .thenReturn(KnowledgeRetrievalResult.empty("EMBEDDING_FAILED"));
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.FAILED, job.getStatus());
        assertEquals("EMBEDDING_FAILED", job.getErrorCode());
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.never())
                .generate(any(AiRequest.class), any(AiModelConfiguration.class));
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatPortOut).saveMessage(messageCaptor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                messageCaptor.getValue().getContent().contains("dịch vụ tìm kiếm"));
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
    void exactFaqPersistsCitationAndAuditWithoutProvider() {
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
        inputMessage.setContent("Ngưng dùng xe có xóa lịch sử không?");

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");
        KnowledgeSearchResult evidence = new KnowledgeSearchResult(
                documentId, chunkId, "Quy trình xe vào và xe ra",
                "Ngưng dùng xe có xóa lịch sử không? Không. Lịch sử vẫn được giữ.",
                null, 1, "Câu hỏi khách hàng thường gặp", new BigDecimal("0.91"));
        KnowledgeRetrievalResult retrieval = new KnowledgeRetrievalResult(
                null, indexVersionId, List.of(evidence), retrievalAuditId,
                new BigDecimal("0.90"), true, "quy trinh xe vao va xe ra", "test-v1");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(accessContextResolver.resolveScopes()).thenReturn(List.of("PUBLIC"));
        when(retrievalService.search(any(KnowledgeRetrievalContext.class), any(String.class),
                eq(List.of("PUBLIC")), any(Integer.class))).thenReturn(retrieval);
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(citationPortOut.saveAll(any(UUID.class), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verifyNoInteractions(modelRouter);
        verify(providerPort, org.mockito.Mockito.never()).generate(any(), any());
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(saved.capture());
        assertEquals("Không. Lịch sử vẫn được giữ.", saved.getValue().getContent());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.ban.vehicle_management.domain.ai.model.AiMessageCitation>> citationCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(citationPortOut).saveAll(any(UUID.class), citationCaptor.capture());
        assertEquals("C1", citationCaptor.getValue().getFirst().label());
        assertEquals(chunkId, citationCaptor.getValue().getFirst().chunkId());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={true,false})
    void canonicalFaqUsesSourceSnapshotWithoutModelIndexOrRun(boolean stillReviewed) {
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
        inputMessage.setContent("Ngưng dùng xe có xóa lịch sử không?");

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");
        KnowledgeSearchResult evidence = new KnowledgeSearchResult(
                documentId, chunkId, "Quy trình xe vào và xe ra",
                "Ngưng dùng xe có xóa lịch sử không? Không. Lịch sử vẫn được giữ.",
                null, 1, "Câu hỏi khách hàng thường gặp", new BigDecimal("0.91"));
        KnowledgeRetrievalResult retrieval = new KnowledgeRetrievalResult(
                null, null, List.of(evidence), retrievalAuditId,
                new BigDecimal("0.90"), true, "quy trinh xe vao va xe ra", "common-v1", "COMMON_FAQ", "source-snapshot", "Không. Lịch sử vẫn được giữ.");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        when(accessContextResolver.resolveScopes()).thenReturn(List.of("PUBLIC"));
        when(retrievalService.search(any(KnowledgeRetrievalContext.class), any(String.class),
                eq(List.of("PUBLIC")), any(Integer.class))).thenReturn(retrieval);
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        if (stillReviewed) when(citationPortOut.saveAll(any(UUID.class), any())).thenAnswer(invocation -> invocation.getArgument(1));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(retrievalService.verifyCommonEvidence(any(),eq(List.of("PUBLIC")),eq(retrieval))).thenReturn(stillReviewed);
        orchestrator.processJob(job);

        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verifyNoInteractions(modelRouter);
        verify(providerPort, org.mockito.Mockito.never()).generate(any(), any());
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(saved.capture());
        verify(aiRunPortOut,org.mockito.Mockito.never()).save(any());
        if (!stillReviewed) {
            assertEquals(com.ban.vehicle_management.domain.ai.model.AssistantResponseStyle.RETRIEVAL_UNAVAILABLE_TEXT,saved.getValue().getContent());
            org.mockito.Mockito.verifyNoInteractions(citationPortOut);return;
        }
        assertEquals("Không. Lịch sử vẫn được giữ.", saved.getValue().getContent());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.ban.vehicle_management.domain.ai.model.AiMessageCitation>> citationCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(citationPortOut).saveAll(any(UUID.class), citationCaptor.capture());
        assertEquals("C1", citationCaptor.getValue().getFirst().label());
        assertEquals(chunkId, citationCaptor.getValue().getFirst().chunkId());
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
        assertTrue(requestCaptor.getValue().messages().getLast().content().contains(inputMessage.getContent()));
        org.junit.jupiter.api.Assertions.assertFalse(systemInstruction.contains("ask them to use the create-ticket confirmation action"));
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
                any(java.time.Instant.class), any(java.time.Instant.class), any(String.class), eq(2)))
                .thenReturn(List.of(job));
        when(chatPortOut.findConversationById(job.getConversationId()))
                .thenThrow(new IllegalStateException("simulated worker failure"));
        when(assistantJobPortOut.save(any(AssistantJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.processDueJobs();

        verify(aiRunPortOut, org.mockito.Mockito.timeout(2000)).failRunningForInputMessage(inputMessageId, "UNEXPECTED_ERROR", true);
        verify(assistantJobPortOut, org.mockito.Mockito.timeout(2000)).save(org.mockito.ArgumentMatchers.argThat(
                saved -> saved.getStatus() == AssistantJobStatus.RETRYING));
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
    void backendOwnedSummarySkipsSecondGenerationAndStillSavesAudit() {
        ModelFlowContext context = arrangePersonalModelFlow();
        AiFunctionCall call = new AiFunctionCall("get_my_subscription_status", "{}", false, null, "call-1", null);
        when(providerPort.generate(any(AiRequest.class), eq(context.configuration)))
                .thenReturn(AiResponse.functionCall(call, "{}", 5, 2, "STOP"));
        when(aiToolCallPortOut.save(any(AiToolCall.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolExecutionService.executeFromAssistantWorker(eq("get_my_subscription_status"), any(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(new AiToolExecutionService.ToolExecutionResult("get_my_subscription_status",
                        "{\"responseText\":\"Vé đang chờ chọn vị trí.\"}", true));
        orchestrator.processJob(context.job);
        assertEquals(AssistantJobStatus.COMPLETED, context.job.getStatus());
        verify(providerPort, org.mockito.Mockito.times(1)).generate(any(AiRequest.class), eq(context.configuration));
        ArgumentCaptor<ChatMessage> message = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(message.capture());
        assertEquals("Vé đang chờ chọn vị trí.", message.getValue().getContent());
        verify(aiToolCallPortOut, org.mockito.Mockito.atLeastOnce()).save(any(AiToolCall.class));
    }

    @Test
    void supportTicketSummarySkipsSecondGenerationAndKeepsAudit() {
        ModelFlowContext context = arrangePersonalModelFlow();
        AiFunctionCall call = new AiFunctionCall(
                "list_my_support_tickets", "{}", false, null, "call-1", "signature-1");
        when(providerPort.generate(any(AiRequest.class), eq(context.configuration)))
                .thenReturn(AiResponse.functionCall(call, "{}", 5, 2, "STOP"));
        when(aiToolCallPortOut.save(any(AiToolCall.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(toolExecutionService.executeFromAssistantWorker(
                eq("list_my_support_tickets"), any(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(new AiToolExecutionService.ToolExecutionResult(
                        "list_my_support_tickets", "{\"responseText\":\"Bạn chưa có phiếu hỗ trợ nào.\"}", false));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.COMPLETED, context.job.getStatus());
        ArgumentCaptor<AiRequest> requestCaptor = ArgumentCaptor.forClass(AiRequest.class);
        org.mockito.Mockito.verify(providerPort, org.mockito.Mockito.times(1))
                .generate(requestCaptor.capture(), eq(context.configuration));
        AiRequest followUp = requestCaptor.getAllValues().getFirst();
        assertEquals(0, followUp.precedingFunctionCalls().size());
        assertEquals(0, followUp.functionResponses().size());
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(messageCaptor.capture());
        assertEquals("Bạn chưa có phiếu hỗ trợ nào.", messageCaptor.getValue().getContent());
    }


    @Test
    void clarificationCompletesWithoutModelOrRetrievalAndFiltersOtherActorsContext() {
        UUID actor = UUID.randomUUID();
        AssistantJob job = new AssistantJob();
        job.setJobId(UUID.randomUUID()); job.setConversationId(UUID.randomUUID());
        job.setInputMessageId(UUID.randomUUID()); job.setAttemptCount(1);
        ChatConversation conversation = new ChatConversation(); conversation.setConversationId(job.getConversationId());
        ChatMessage input = new ChatMessage(); input.setMessageId(job.getInputMessageId());
        input.setSenderAccountId(actor); input.setConversationId(job.getConversationId());
        input.setMessageType(ChatMessageType.TEXT); input.setContent("loi");
        input.setCreatedAt(java.time.Instant.parse("2026-11-19T01:00:00Z"));
        ChatMessage other = new ChatMessage(); other.setMessageId(UUID.randomUUID());
        other.setSenderAccountId(UUID.randomUUID()); other.setMessageType(ChatMessageType.TEXT);
        other.setContent("Làm sao đăng nhập?");
        when(chatPortOut.findConversationById(job.getConversationId())).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(job.getInputMessageId())).thenReturn(Optional.of(input));
        when(chatPortOut.findMessageHistory(job.getConversationId(), input.getCreatedAt(), 8)).thenReturn(List.of(input, other));
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        orchestrator.processJob(job);
        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verifyNoInteractions(retrievalService, modelRouter, toolExecutionService);
        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(saved.capture());
        assertTrue(saved.getValue().getContent().contains("chức năng"));
    }

    @Test
    void dependentKnowledgeFetchesOnlyPastQuestionsFromSameActorBeforeRetrieval() {
        UUID actor = UUID.randomUUID();
        AssistantJob job = new AssistantJob();
        job.setJobId(UUID.randomUUID()); job.setConversationId(UUID.randomUUID());
        job.setInputMessageId(UUID.randomUUID()); job.setAttemptCount(1);
        ChatConversation conversation = new ChatConversation(); conversation.setConversationId(job.getConversationId());
        ChatMessage input = new ChatMessage(); input.setMessageId(job.getInputMessageId());
        input.setSenderAccountId(actor); input.setConversationId(job.getConversationId());
        input.setMessageType(ChatMessageType.TEXT); input.setContent("Nếu dùng vé đăng ký thì cần làm gì?");
        input.setCreatedAt(java.time.Instant.parse("2026-11-23T01:00:00Z"));
        ChatMessage previous = new ChatMessage(); previous.setMessageId(UUID.randomUUID());
        previous.setSenderAccountId(actor); previous.setMessageType(ChatMessageType.TEXT);
        previous.setCreatedAt(input.getCreatedAt().minusSeconds(1)); previous.setContent("Vào bãi bằng vé đăng ký như thế nào?");
        ChatMessage future = new ChatMessage(); future.setMessageId(UUID.randomUUID());
        future.setSenderAccountId(actor); future.setMessageType(ChatMessageType.TEXT);
        future.setCreatedAt(input.getCreatedAt().plusSeconds(1)); future.setContent("Cách áp voucher?");
        when(chatPortOut.findConversationById(job.getConversationId())).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(job.getInputMessageId())).thenReturn(Optional.of(input));
        when(chatPortOut.findMessageHistory(job.getConversationId(), input.getCreatedAt(), 8)).thenReturn(List.of(input, future, previous));
        when(accessContextResolver.resolveScopes()).thenReturn(List.of("PUBLIC"));
        when(retrievalService.search(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new KnowledgeRetrievalResult(null, UUID.randomUUID(), List.of(), UUID.randomUUID(),
                        BigDecimal.ZERO, false, "test", "INSUFFICIENT_EVIDENCE"));
        when(chatPortOut.saveMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantJobPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        orchestrator.processJob(job);
        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(retrievalService).search(any(), query.capture(), eq(List.of("PUBLIC")), org.mockito.ArgumentMatchers.anyInt());
        assertTrue(query.getValue().contains("xe vào"));
        org.junit.jupiter.api.Assertions.assertFalse(query.getValue().contains("voucher"));
        assertEquals(AssistantJobStatus.COMPLETED, job.getStatus());
        org.mockito.Mockito.verifyNoInteractions(modelRouter, toolExecutionService);
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "Thêm xe giúp tôi,Xe của tôi",
            "Gia hạn vé giúp tôi,Vé tháng của tôi",
            "Báo mất thẻ giúp tôi,nhân viên bãi xe",
            "Sửa lịch sử gửi xe giúp tôi,không sửa",
            "Thẻ của tôi đang bị khóa không?,chưa tra cứu trực tiếp",
            "Lịch sử hôm qua của tôi có phí bao nhiêu?,Khoảng ngày",
            "Xem các lượt gửi xe của tôi hôm qua,Khoảng ngày",
            "Hồ sơ của tôi đã duyệt chưa?,Hồ sơ cá nhân"
            ,"Hoàn tiền giúp tôi,chưa thực hiện hoàn tiền"
            ,"Thanh toán giúp tôi,chưa thanh toán"
            ,"Đặt chỗ giúp tôi,Đặt chỗ trước"
            ,"Hủy đặt chỗ giúp tôi,Đặt chỗ trước"
            ,"Xem hóa đơn của tôi,chưa tra cứu trực tiếp"
            ,"Áp mã giảm giá giúp tôi,chưa thanh toán"
            ,"Thanh toán hóa đơn giúp tôi,chưa thanh toán"
            ,"Sửa hóa đơn giúp tôi,chưa thanh toán"
    })
    void unsupportedBusinessOperationExplainsCorrectScreenWithoutModelOrMutation(String question, String expected) {
        AssistantJob job = new AssistantJob(); job.setJobId(UUID.randomUUID());
        job.setConversationId(UUID.randomUUID()); job.setInputMessageId(UUID.randomUUID()); job.setAttemptCount(1);
        ChatConversation conversation = new ChatConversation(); conversation.setConversationId(job.getConversationId());
        ChatMessage input = new ChatMessage(); input.setMessageId(job.getInputMessageId());
        input.setConversationId(job.getConversationId()); input.setSenderAccountId(UUID.randomUUID());
        input.setMessageType(ChatMessageType.TEXT); input.setContent(question);
        when(chatPortOut.findConversationById(job.getConversationId())).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(job.getInputMessageId())).thenReturn(Optional.of(input));
        when(chatPortOut.saveConversation(any(ChatConversation.class))).thenAnswer(call -> call.getArgument(0));
        when(chatPortOut.saveMessage(any(ChatMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(assistantJobPortOut.save(any(AssistantJob.class))).thenAnswer(call -> call.getArgument(0));
        orchestrator.processJob(job);
        assertEquals(AssistantJobStatus.COMPLETED,job.getStatus());
        ArgumentCaptor<ChatMessage> output = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(output.capture());
        org.junit.jupiter.api.Assertions.assertTrue(output.getValue().getContent().contains(expected));
        verify(providerPort,org.mockito.Mockito.never()).generate(any(),any());
        org.mockito.Mockito.verifyNoInteractions(toolExecutionService,retrievalService);
    }

    @Test
    void slowJobDoesNotBlockAnotherActorAndBusyWorkersDoNotOverclaim() throws Exception {
        var slowEntered = new java.util.concurrent.CountDownLatch(1);
        var fastEntered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        AssistantJob slow = new AssistantJob(); slow.setJobId(UUID.randomUUID());
        slow.setConversationId(UUID.randomUUID()); slow.setInputMessageId(UUID.randomUUID());
        AssistantJob fast = new AssistantJob(); fast.setJobId(UUID.randomUUID());
        fast.setConversationId(UUID.randomUUID()); fast.setInputMessageId(UUID.randomUUID());
        ChatConversation slowConversation = new ChatConversation(); slowConversation.setConversationId(slow.getConversationId());
        ChatConversation fastConversation = new ChatConversation(); fastConversation.setConversationId(fast.getConversationId());
        ChatMessage slowInput = new ChatMessage(); slowInput.setMessageId(slow.getInputMessageId());
        slowInput.setConversationId(slow.getConversationId()); slowInput.setSenderAccountId(UUID.randomUUID());
        slowInput.setMessageType(ChatMessageType.TEXT); slowInput.setContent("Xin chào");
        ChatMessage fastInput = new ChatMessage(); fastInput.setMessageId(fast.getInputMessageId());
        fastInput.setConversationId(fast.getConversationId()); fastInput.setSenderAccountId(UUID.randomUUID());
        fastInput.setMessageType(ChatMessageType.TEXT); fastInput.setContent("Xin chào");
        when(chatPortOut.findMessageById(slow.getInputMessageId())).thenReturn(Optional.of(slowInput));
        when(chatPortOut.findMessageById(fast.getInputMessageId())).thenReturn(Optional.of(fastInput));
        when(chatPortOut.saveMessage(any())).thenAnswer(call -> call.getArgument(0));
        when(chatPortOut.saveConversation(any())).thenAnswer(call -> call.getArgument(0));
        when(assistantJobPortOut.save(any())).thenAnswer(call -> call.getArgument(0));
        when(assistantJobPortOut.claimDueJobs(any(),any(),any(String.class),eq(2))).thenReturn(List.of(slow,fast));
        when(chatPortOut.findConversationById(slow.getConversationId())).thenAnswer(call -> {
            slowEntered.countDown(); release.await(5,java.util.concurrent.TimeUnit.SECONDS); return Optional.of(slowConversation);
        });
        when(chatPortOut.findConversationById(fast.getConversationId())).thenAnswer(call -> {
            fastEntered.countDown(); release.await(5,java.util.concurrent.TimeUnit.SECONDS); return Optional.of(fastConversation);
        });
        try {
            orchestrator.processDueJobs();
            org.junit.jupiter.api.Assertions.assertTrue(slowEntered.await(2,java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertTrue(fastEntered.await(2,java.util.concurrent.TimeUnit.SECONDS),"A blocked first job must not hold the second job in a serial queue");
            orchestrator.processDueJobs();
            verify(assistantJobPortOut,org.mockito.Mockito.times(1)).claimDueJobs(any(),any(),any(String.class),eq(2));
        } finally { release.countDown(); }
        verify(assistantJobPortOut,org.mockito.Mockito.timeout(2000).times(2)).save(
                org.mockito.ArgumentMatchers.argThat(saved -> saved.getStatus() == AssistantJobStatus.COMPLETED));
        verify(providerPort,org.mockito.Mockito.never()).generate(any(),any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false,true})
    void deniedReadStopsAfterOneModelCallAndDoesNotDiscloseResourceExistence(boolean deniedAtExecution) {
        ModelFlowContext context = arrangePersonalModelFlow();
        AiFunctionCall call = new AiFunctionCall("list_my_support_tickets","{}",false,null,"call-1",null);
        when(providerPort.generate(any(AiRequest.class),eq(context.configuration)))
                .thenReturn(AiResponse.functionCall(call,"{}",5,2,"STOP"));
        when(aiToolCallPortOut.save(any(AiToolCall.class))).thenAnswer(invocation -> invocation.getArgument(0));
        if (deniedAtExecution) {
            when(toolExecutionService.executeFromAssistantWorker(eq("list_my_support_tickets"),any()))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("revoked"));
        } else {
            org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("revoked"))
                    .when(currentAccountPortIn).requirePermission("SUPPORT_TICKET_READ_OWN");
        }
        orchestrator.processJob(context.job);
        assertEquals(AssistantJobStatus.COMPLETED,context.job.getStatus());
        verify(providerPort,org.mockito.Mockito.times(1)).generate(any(),any());
        ArgumentCaptor<ChatMessage> message = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(message.capture());
        org.junit.jupiter.api.Assertions.assertTrue(message.getValue().getContent().contains("tài khoản hiện tại"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "Xem danh sách xe của tôi,list_my_vehicles",
        "Vé tháng của tôi còn hạn không?,get_my_subscription_status",
        "Xem lịch sử gửi xe của tôi,list_my_parking_history",
        "Xem phiếu của tôi,list_my_support_tickets"
    })
    void explicitOwnListsUseAuthorizedToolAndAuditWithZeroProviderCalls(String question, String tool) {
        ModelFlowContext context = arrangePersonalModelFlow(question);
        when(aiToolCallPortOut.save(any(AiToolCall.class))).thenAnswer(call -> call.getArgument(0));
        when(toolExecutionService.executeFromAssistantWorker(eq(tool), any()))
                .thenReturn(new AiToolExecutionService.ToolExecutionResult(tool,
                        "{\"responseText\":\"Thông tin của bạn:\\nChưa có dữ liệu.\"}", true));

        orchestrator.processJob(context.job);

        assertEquals(AssistantJobStatus.COMPLETED, context.job.getStatus());
        org.mockito.Mockito.verifyNoInteractions(modelRouter, retrievalService);
        verify(providerPort, org.mockito.Mockito.never()).generate(any(), any());
        verify(toolExecutionService).executeFromAssistantWorker(eq(tool), any());
        ArgumentCaptor<AiToolCall> audit = ArgumentCaptor.forClass(AiToolCall.class);
        verify(aiToolCallPortOut, org.mockito.Mockito.times(2)).save(audit.capture());
        assertEquals(com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus.SUCCEEDED,
                audit.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertNull(audit.getValue().getRunId());
        ArgumentCaptor<ChatMessage> answer = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(answer.capture());
        assertEquals("Thông tin của bạn:\nChưa có dữ liệu.", answer.getValue().getContent());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void directOwnReadNeverFallsBackToModelWhenPermissionIsRevoked(boolean atExecution) {
        ModelFlowContext context = arrangePersonalModelFlow("Xem danh sách xe của tôi");
        when(aiToolCallPortOut.save(any(AiToolCall.class))).thenAnswer(call -> call.getArgument(0));
        if (atExecution) {
            when(toolExecutionService.executeFromAssistantWorker(eq("list_my_vehicles"), any()))
                    .thenThrow(new org.springframework.security.access.AccessDeniedException("revoked"));
        } else {
            org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("revoked"))
                    .when(currentAccountPortIn).requirePermission("CUSTOMER_VEHICLE_READ_OWN");
        }
        orchestrator.processJob(context.job);
        assertEquals(AssistantJobStatus.COMPLETED, context.job.getStatus());
        verify(providerPort, org.mockito.Mockito.never()).generate(any(), any());
        if (!atExecution) org.mockito.Mockito.verifyNoInteractions(toolExecutionService);
        ArgumentCaptor<ChatMessage> answer = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatPortOut).saveMessage(answer.capture());
        org.junit.jupiter.api.Assertions.assertTrue(answer.getValue().getContent().contains("tài khoản hiện tại"));
    }

    @Test
    void modelRequestAlwaysIncludesCurrentInputOnceAndExcludesOtherActorsAndFutureTurns() {
        ModelFlowContext context = arrangePersonalModelFlow();
        ChatMessage input = chatPortOut.findMessageById(context.job.getInputMessageId()).orElseThrow();
        ChatMessage otherActor = new ChatMessage();
        otherActor.setMessageId(UUID.randomUUID());
        otherActor.setSenderAccountId(UUID.randomUUID());
        otherActor.setMessageType(ChatMessageType.TEXT);
        otherActor.setContent("other-actor-private-question");
        otherActor.setCreatedAt(input.getCreatedAt().minusSeconds(2));
        ChatMessage future = new ChatMessage();
        future.setMessageId(UUID.randomUUID());
        future.setSenderAccountId(input.getSenderAccountId());
        future.setMessageType(ChatMessageType.TEXT);
        future.setContent("future-question");
        future.setCreatedAt(input.getCreatedAt().plusSeconds(1));
        when(chatPortOut.findMessageHistory(context.job.getConversationId(), input.getCreatedAt(), properties.getRecentMessageLimit()))
                .thenReturn(List.of(future, input, otherActor));
        when(providerPort.generate(any(), eq(context.configuration)))
                .thenReturn(AiResponse.success("{\"responseText\":\"Không có dữ liệu\"}", "{}", 1, 1));
        orchestrator.processJob(context.job);
        ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
        verify(providerPort).generate(request.capture(), eq(context.configuration));
        assertEquals(1, request.getValue().messages().size());
        assertEquals(input.getContent(), request.getValue().messages().getFirst().content());
    }

    private ModelFlowContext arrangePersonalModelFlow() {
        return arrangePersonalModelFlow("Xem phiếu của tôi đang xử lý");
    }

    private ModelFlowContext arrangePersonalModelFlow(String question) {
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
        inputMessage.setContent(question);
        inputMessage.setCreatedAt(java.time.Instant.parse("2026-11-19T04:00:00Z"));

        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-test");

        when(aiRunPortOut.existsSuccessfulRunForInputMessage(inputMessageId)).thenReturn(false);
        when(chatPortOut.findConversationById(conversationId)).thenReturn(Optional.of(conversation));
        when(chatPortOut.findMessageById(inputMessageId)).thenReturn(Optional.of(inputMessage));
        if (com.ban.vehicle_management.domain.ai.model.AssistantOwnReadRouter.resolve(question).isEmpty()) {
            when(chatPortOut.findMessageHistory(conversationId, inputMessage.getCreatedAt(), properties.getRecentMessageLimit()))
                    .thenReturn(List.of(inputMessage));
            when(modelRouter.resolveCandidates(AiUseCase.SUPPORT_CHAT, conversationId, senderAccountId, 1))
                    .thenReturn(List.of(configuration));
            when(aiRunPortOut.save(any(AiRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }
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
