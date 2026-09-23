package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled worker that advances knowledge index builds. Building is decoupled from the
 * build request HTTP call: the request snaps the expected chunk count and returns, while
 * this worker embeds chunks in batches until the version is READY or FAILED.
 */
@Component
public class KnowledgeIndexBuildScheduler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexBuildScheduler.class);

    private final EmbeddingProperties properties;
    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final KnowledgeIndexBuildService buildService;

    public KnowledgeIndexBuildScheduler(
            EmbeddingProperties properties,
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            KnowledgeIndexBuildService buildService
    ) {
        this.properties = properties;
        this.indexVersionPortOut = indexVersionPortOut;
        this.buildService = buildService;
    }

    @Scheduled(
            fixedDelayString = "${app.ai.embedding.build-fixed-delay-ms:5000}",
            initialDelayString = "${app.ai.embedding.build-initial-delay-ms:10000}"
    )
    public void buildPendingIndexVersions() {
        if (!properties.isEnabled()) {
            return;
        }
        List<KnowledgeIndexVersion> buildingVersions = indexVersionPortOut.findByStatus(KnowledgeIndexVersionStatus.BUILDING);
        for (KnowledgeIndexVersion version : buildingVersions) {
            try {
                buildService.process(version.getIndexVersionId());
            } catch (Exception exception) {
                log.error("Knowledge index build failed for version {}", version.getIndexVersionId(), exception);
            }
        }
    }
}