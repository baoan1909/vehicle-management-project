package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionJobUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private KnowledgeIngestionJobPortOut jobPortOut;
    @Mock
    private KnowledgeDocumentPortOut documentPortOut;

    private KnowledgeIngestionJobUseCaseImpl useCase;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new KnowledgeIngestionJobUseCaseImpl(currentAccountPortIn, jobPortOut, documentPortOut);
        lenient().when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actor);
        lenient().when(jobPortOut.save(any(KnowledgeIngestionJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void retryJobShouldRequeueWhenAttemptsRemain() {
        KnowledgeIngestionJob job = failedJob(1, 3);
        when(jobPortOut.findByIdForUpdate(job.getIngestionJobId())).thenReturn(Optional.of(job));
        KnowledgeDocument document = KnowledgeDocument.newVersion(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), "Tiêu đề",
                "key", "a.pdf", "pdf", "application/pdf", 10L, "sha", 1,
                com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope.PUBLIC, Instant.now());
        document.fail("EXTRACT_FAILED", Instant.now());
        when(documentPortOut.findById(job.getDocumentId())).thenReturn(Optional.of(document));

        KnowledgeIngestionJob retried = useCase.retryJob(job.getIngestionJobId());

        assertEquals(KnowledgeIngestionJobStatus.PENDING, retried.getStatus());
        assertEquals(0, retried.getAttemptCount());
        assertTrue(document.getStatus() == com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus.PENDING);
        verify(jobPortOut).save(job);
        verify(documentPortOut).save(document);
    }

    @Test
    void retryJobShouldAllowManualRetryAfterAutomaticAttemptsAreExhausted() {
        KnowledgeIngestionJob job = failedJob(1, 3);
        job.setStatus(KnowledgeIngestionJobStatus.FAILED);
        job.setAttemptCount(3);
        when(jobPortOut.findByIdForUpdate(job.getIngestionJobId())).thenReturn(Optional.of(job));

        KnowledgeIngestionJob retried = useCase.retryJob(job.getIngestionJobId());

        assertEquals(KnowledgeIngestionJobStatus.PENDING, retried.getStatus());
        assertEquals(0, retried.getAttemptCount());
    }

    @Test
    void jobDetailShouldThrowWhenMissing() {
        when(jobPortOut.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(
                com.ban.vehicle_management.shared.exception.NotFoundException.class,
                () -> useCase.jobDetail(UUID.randomUUID())
        );
    }

    @Test
    void listJobsShouldReturnAll() {
        when(jobPortOut.findAll()).thenReturn(java.util.List.of(failedJob(1, 3)));

        assertEquals(1, useCase.listJobs().size());
    }

    private KnowledgeIngestionJob failedJob(int attempts, int maxAttempts) {
        KnowledgeIngestionJob job = KnowledgeIngestionJob.create(
                UUID.randomUUID(), maxAttempts, "key-" + UUID.randomUUID(), actor, Instant.now());
        job.claim("worker-1", attempts, Instant.now(), Instant.now().plusSeconds(60));
        job.advanceStage(IngestionStage.CHUNKING, 50, Instant.now());
        job.fail("EXTRACT_FAILED", "Không trích xuất được văn bản", Instant.now());
        return job;
    }
}
