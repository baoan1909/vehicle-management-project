package com.ban.vehicle_management.application.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionScheduler.class);

    private final KnowledgeIngestionProperties properties;
    private final KnowledgeIngestionOrchestrator orchestrator;

    public KnowledgeIngestionScheduler(
            KnowledgeIngestionProperties properties,
            KnowledgeIngestionOrchestrator orchestrator) {
        this.properties = properties;
        this.orchestrator = orchestrator;
    }

    @Scheduled(
            fixedDelayString = "${app.ai.ingestion.poll-fixed-delay-ms:5000}",
            initialDelayString = "${app.ai.ingestion.poll-initial-delay-ms:15000}"
    )
    public void poll() {
        try {
            orchestrator.pollDueJobs();
        } catch (Exception exception) {
            log.error("Knowledge ingestion poll failed", exception);
        }
    }
}