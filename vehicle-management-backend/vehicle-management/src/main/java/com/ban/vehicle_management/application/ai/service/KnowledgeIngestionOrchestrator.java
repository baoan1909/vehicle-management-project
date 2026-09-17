package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Claims due ingestion jobs and hands each to the worker. Claiming and processing are
 * deliberately split: the claim is a short transaction (FOR UPDATE SKIP LOCKED) and the
 * worker runs each stage in its own short transaction. The worker id is fixed for the
 * lifetime of this application instance so heartbeats renew the correct lease; a fresh
 * random id per poll would silently break lease ownership mid-job.
 */
@Component
public class KnowledgeIngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionOrchestrator.class);

    private final KnowledgeIngestionProperties properties;
    private final KnowledgeIngestionJobPortOut jobPortOut;
    private final KnowledgeIngestionWorker worker;
    private final String workerId;

    public KnowledgeIngestionOrchestrator(
            KnowledgeIngestionProperties properties,
            KnowledgeIngestionJobPortOut jobPortOut,
            KnowledgeIngestionWorker worker) {
        this.properties = properties;
        this.jobPortOut = jobPortOut;
        this.worker = worker;
        this.workerId = properties.getWorkerIdPrefix() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void pollDueJobs() {
        if (!properties.isEnabled()) {
            return;
        }
        Instant now = Instant.now();
        List<KnowledgeIngestionJob> due = jobPortOut.claimDueJobs(
                now,
                now.plus(properties.getLockDuration()),
                workerId,
                properties.getClaimLimit(),
                properties.getRetryInitialDelay());
        for (KnowledgeIngestionJob job : due) {
            try {
                worker.process(job);
            } catch (Exception exception) {
                log.error("Knowledge ingestion worker failed for job {}", job.getIngestionJobId(), exception);
            }
        }
    }
}