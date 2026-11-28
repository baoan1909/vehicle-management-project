package com.ban.vehicle_management.application.ai.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonKnowledgeSchedulerTest {
    @Mock AssistantJobPortOut jobs;
    @Spy AiAssistantProperties properties=new AiAssistantProperties();
    @Spy List<AiProviderPortOut> providers=new ArrayList<>();
    @InjectMocks AssistantOrchestrator orchestrator;
    @AfterEach void closeWorker() { orchestrator.close(); }
    @Test void providerPolicyAllowsJobsWithoutRuntimeModeDatabaseOrLocalModel() {
        when(properties.isProviderCallAllowed()).thenReturn(true);
        when(jobs.claimDueJobs(any(),any(),any(),anyInt())).thenReturn(List.of());
        clearInvocations(providers); // Constructor registers providers; scheduling itself must not call one.
        orchestrator.processDueJobs();
        verify(jobs).claimDueJobs(any(),any(),any(),anyInt());
        verifyNoInteractions(providers);
    }
    @Test void disabledProviderPolicyDoesNotClaimJobs() {
        when(properties.isProviderCallAllowed()).thenReturn(false);
        orchestrator.processDueJobs();
        verifyNoInteractions(jobs);
    }
}
