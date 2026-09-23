package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIngestionJobEventPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.ai.KnowledgeIngestionJobPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIngestionJobEventRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIngestionJobRepository;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionJobPersistenceAdapterTest {

    @Mock
    private KnowledgeIngestionJobRepository repository;

    @Mock
    private KnowledgeIngestionJobPersistenceMapper mapper;

    @Mock
    private KnowledgeIngestionJobEventRepository eventRepository;

    @Mock
    private KnowledgeIngestionJobEventPersistenceMapper eventMapper;

    @InjectMocks
    private KnowledgeIngestionJobPersistenceAdapter adapter;

    @Test
    void shouldQueryOpenJobsUsingEntityEnumType() {
        UUID documentId = UUID.randomUUID();
        List<KnowledgeIngestionJobStatus> openStatuses = List.of(
                KnowledgeIngestionJobStatus.PENDING,
                KnowledgeIngestionJobStatus.PROCESSING,
                KnowledgeIngestionJobStatus.RETRYING,
                KnowledgeIngestionJobStatus.REVIEW);
        when(repository.findByDocumentIdAndStatusInOrderByCreatedAtDesc(documentId, openStatuses))
                .thenReturn(List.of());

        assertTrue(adapter.findOpenByDocumentId(documentId).isEmpty());

        verify(repository).findByDocumentIdAndStatusInOrderByCreatedAtDesc(documentId, openStatuses);
    }

    @Test
    void shouldQueryJobsByEnumStatus() {
        when(repository.findByStatusOrderByCreatedAtAsc(KnowledgeIngestionJobStatus.RETRYING))
                .thenReturn(List.of());

        assertTrue(adapter.findByStatus(KnowledgeIngestionJobStatus.RETRYING).isEmpty());

        verify(repository).findByStatusOrderByCreatedAtAsc(KnowledgeIngestionJobStatus.RETRYING);
    }
}
