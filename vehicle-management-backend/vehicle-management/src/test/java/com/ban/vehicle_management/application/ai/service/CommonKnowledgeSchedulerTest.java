package com.ban.vehicle_management.application.ai.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.*;
import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommonKnowledgeSchedulerTest {
    @Mock AssistantJobPortOut jobs;
    @Mock AiRuntimeModeService runtime;
    @Spy AiAssistantProperties properties=new AiAssistantProperties();
    @Spy List<AiProviderPortOut> providers=new ArrayList<>();
    @InjectMocks AssistantOrchestrator orchestrator;
    @AfterEach void closeWorker() { orchestrator.close(); }
    @Test void preparingIndexDoesNotBlockCommonJobsButSwitchingStillDoes() {
        org.springframework.test.util.ReflectionTestUtils.setField(orchestrator,"runtimeMode",runtime);
        when(runtime.callsAllowed(properties)).thenReturn(true);
        var mode=AiRuntimeMode.QWEN_LOCAL;
        when(runtime.current()).thenReturn(new AiRuntimeState(mode,mode,8,"PREPARING",null,Instant.now()));
        when(jobs.claimDueJobs(any(),any(),any(),anyInt())).thenReturn(List.of());
        orchestrator.processDueJobs();
        when(runtime.current()).thenReturn(new AiRuntimeState(mode,mode,9,"SWITCHING",null,Instant.now()));
        orchestrator.processDueJobs();
        verify(jobs).claimDueJobs(any(),any(),any(),anyInt());
        verify(runtime,never()).acquire(any());
    }
}
