package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.KnowledgeIngestionJobPortIn;
import com.ban.vehicle_management.application.ai.query.KnowledgeIngestionJobQuery;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class KnowledgeIngestionJobUseCaseImpl implements KnowledgeIngestionJobPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final KnowledgeIngestionJobPortOut jobPortOut;
    private final KnowledgeDocumentPortOut documentPortOut;

    public KnowledgeIngestionJobUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            KnowledgeIngestionJobPortOut jobPortOut,
            KnowledgeDocumentPortOut documentPortOut) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.jobPortOut = jobPortOut;
        this.documentPortOut = documentPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIngestionJob> listJobs() {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return jobPortOut.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeIngestionJob> listJobs(KnowledgeIngestionJobQuery query, Pageable pageable) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return jobPortOut.findAll(query, KnowledgePagePolicy.normalize(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeIngestionJob jobDetail(UUID ingestionJobId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return jobPortOut.findById(ingestionJobId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tiến trình xử lý"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIngestionJobEvent> jobEvents(UUID ingestionJobId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return jobPortOut.findEventsByJobId(ingestionJobId);
    }

    @Override
    @Transactional
    public KnowledgeIngestionJob retryJob(UUID ingestionJobId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        KnowledgeIngestionJob job = jobPortOut.findByIdForUpdate(ingestionJobId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tiến trình xử lý"));
        if (job.getStatus() != KnowledgeIngestionJobStatus.FAILED) {
            throw new BadRequestException("Chỉ tiến trình thất bại mới có thể được chạy lại");
        }
        Instant now = Instant.now();
        job.resetForRetry(now);
        jobPortOut.save(job);
        documentPortOut.findById(job.getDocumentId()).ifPresent(document -> {
            document.resetForNewAttempt(now);
            documentPortOut.save(document);
        });
        return job;
    }
}
