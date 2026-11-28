package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.ai.command.CreateKnowledgeIndexVersionCommand;
import com.ban.vehicle_management.application.ai.port.out.*;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.*;
import com.ban.vehicle_management.shared.enumeration.ai.*;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexPendingCandidateTest {
    @Mock CurrentAccountPortIn currentAccountPortIn;
    @Mock KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock AiModelConfigurationPortOut configurationPortOut;
    @Mock KnowledgeChunkPortOut chunkPortOut;
    @Mock KnowledgeEmbeddingPortOut embeddingPortOut;
    @Mock EmbeddingPromptFormatter promptFormatter;
    @Mock EmbeddingProperties embeddingProperties;
    @InjectMocks KnowledgeIndexVersionUseCaseImpl useCase;

    @ParameterizedTest
    @EnumSource(value = KnowledgeIndexVersionStatus.class, names = {"DRAFT", "BUILDING"})
    void anExistingCandidateIsAConflictRatherThanADatabaseConstraintFailure(KnowledgeIndexVersionStatus status) {
        UUID configurationId = UUID.randomUUID();
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(configurationId); configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setOutputDimension(768);
        when(configurationPortOut.findById(configurationId)).thenReturn(Optional.of(configuration));
        KnowledgeIndexVersion pending = new KnowledgeIndexVersion(); pending.setStatus(status);
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.of(pending));

        assertThrows(ConflictException.class, () -> useCase.createDraft(new CreateKnowledgeIndexVersionCommand(configurationId, "NEW_INDEX")));
        InOrder order = inOrder(indexVersionPortOut);
        order.verify(indexVersionPortOut).lockCandidateAdvisory();
        order.verify(indexVersionPortOut).findFirstPendingCandidate();
        verify(indexVersionPortOut, never()).save(any());
    }
}
