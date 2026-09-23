package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.query.KnowledgeIngestionJobQuery;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeIngestionJobPortIn {

    List<KnowledgeIngestionJob> listJobs();

    Page<KnowledgeIngestionJob> listJobs(KnowledgeIngestionJobQuery query, Pageable pageable);

    KnowledgeIngestionJob jobDetail(UUID ingestionJobId);

    List<KnowledgeIngestionJobEvent> jobEvents(UUID ingestionJobId);

    KnowledgeIngestionJob retryJob(UUID ingestionJobId);
}
