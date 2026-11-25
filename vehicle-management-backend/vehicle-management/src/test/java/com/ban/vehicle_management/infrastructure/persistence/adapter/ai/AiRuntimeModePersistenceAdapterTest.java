package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ban.vehicle_management.application.ai.service.AiLocalBusyException;
import com.ban.vehicle_management.application.ai.service.AiRuntimeModeChangedException;
import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiRuntimeModePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRuntimeModeEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiRuntimeModeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AssistantJobRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class AiRuntimeModePersistenceAdapterTest {
    private static final long DISPATCH_LOCK = 641905241201L;
    private static final long LOCAL_INFERENCE_LOCK = 641905241202L;
    private static final UUID ACTOR = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Mock AiRuntimeModeRepository repository;
    @Mock AssistantJobRepository jobs;
    @Mock KnowledgeIndexVersionRepository indexes;
    @Mock AiRuntimeModePersistenceMapper mapper;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks AiRuntimeModePersistenceAdapter adapter;

    private void transactions() {
        when(transactionManager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
    }

    private AiRuntimeState state(AiRuntimeMode mode, AiRuntimeMode target, String status) {
        return new AiRuntimeState(mode, target, 7, status, ACTOR, Instant.parse("2026-11-25T00:00:00Z"));
    }

    private AiRuntimeModeEntity current(AiRuntimeState state) {
        var entity = new AiRuntimeModeEntity();
        when(repository.findCurrent()).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(state);
        return entity;
    }

    @Test
    void dispatchTransactionRemainsOpenUntilLeaseClosesAndDoesNotUseCallerThread() {
        transactions();
        current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.GOOGLE_API, "READY"));
        var lockThread = new AtomicReference<Thread>();
        when(repository.tryAcquireSharedLock(DISPATCH_LOCK)).thenAnswer(call -> {
            lockThread.set(Thread.currentThread());
            return true;
        });
        var lease = adapter.acquire(AiProvider.GEMINI, 7);
        try {
            assertEquals(7, lease.epoch());
            assertNotSame(Thread.currentThread(), lockThread.get());
            assertTrue(lockThread.get().isVirtual());
            verify(transactionManager, times(1)).commit(any());
            verify(repository, never()).tryAcquireExclusiveLock(LOCAL_INFERENCE_LOCK);
        } finally {
            lease.close();
        }
        verify(transactionManager, times(2)).commit(any());
        lease.close();
        verify(transactionManager, times(2)).commit(any());
    }

    @Test
    void localBusyRollsBackLeaseTransactionBeforeReturningToCaller() {
        transactions();
        current(state(AiRuntimeMode.QWEN_LOCAL, AiRuntimeMode.QWEN_LOCAL, "PREPARING"));
        when(repository.tryAcquireSharedLock(DISPATCH_LOCK)).thenReturn(true);
        when(repository.tryAcquireExclusiveLock(LOCAL_INFERENCE_LOCK)).thenReturn(false);
        assertThrows(AiLocalBusyException.class, () -> adapter.acquire(AiProvider.LM_STUDIO, 7));
        verify(transactionManager).rollback(any());
        verify(transactionManager, times(1)).commit(any());
    }

    @Test
    void providerMismatchDoesNotAcquireAnyDispatchLock() {
        transactions();
        current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.GOOGLE_API, "READY"));
        assertThrows(AiRuntimeModeChangedException.class, () -> adapter.acquire(AiProvider.LM_STUDIO, 7));
        verify(repository, never()).tryAcquireSharedLock(anyLong());
        verify(repository, never()).tryAcquireExclusiveLock(anyLong());
    }

    @Test
    void modeChangeBetweenPreflightAndLockRollsBackWithoutDispatching() {
        transactions();
        var entity = new AiRuntimeModeEntity();
        when(repository.findCurrent()).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(
                state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.GOOGLE_API, "READY"),
                state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.QWEN_LOCAL, "SWITCHING"));
        when(repository.tryAcquireSharedLock(DISPATCH_LOCK)).thenReturn(true);
        assertThrows(AiRuntimeModeChangedException.class, () -> adapter.acquire(AiProvider.GEMINI, 7));
        verify(transactionManager).rollback(any());
    }

    @Test
    void failedCompareAndSetDoesNotRecordSuccessfulSwitchRequest() {
        transactions();
        current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.GOOGLE_API, "READY"));
        when(repository.requestSwitch("QWEN_LOCAL", 7, ACTOR)).thenReturn(0);
        assertThrows(BadRequestException.class, () -> adapter.requestSwitch(AiRuntimeMode.QWEN_LOCAL, 7, ACTOR));
        verify(repository, never()).recordModeEvent(anyString(), anyString(), anyLong(), any(), anyString());
        verify(transactionManager).rollback(any());
    }

    @Test
    void oldDispatchLeasePreventsSwitchAcknowledgement() {
        transactions();
        current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.QWEN_LOCAL, "SWITCHING"));
        when(repository.tryAcquireExclusiveLock(DISPATCH_LOCK)).thenReturn(false);
        adapter.advanceSwitch();
        verify(repository, never()).findCurrentForUpdate();
        verifyNoInteractions(jobs, indexes);
        verify(repository, never()).completeSwitch(anyLong(), anyString());
    }

    @Test
    void acknowledgementAndJobFencingShareOneTransaction() {
        transactions();
        var entity = current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.QWEN_LOCAL, "SWITCHING"));
        when(repository.tryAcquireExclusiveLock(DISPATCH_LOCK)).thenReturn(true);
        when(repository.findCurrentForUpdate()).thenReturn(Optional.of(entity));
        when(indexes.findByProviderAndStatusAndActivatedAtIsNotNull(AiProvider.LM_STUDIO, KnowledgeIndexVersionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(repository.completeSwitch(7, "PREPARING")).thenReturn(1);
        adapter.advanceSwitch();
        var order = inOrder(repository, jobs, transactionManager);
        order.verify(repository).completeSwitch(7, "PREPARING");
        order.verify(jobs).failJobsFromOtherRuntimeEpochs(7);
        order.verify(repository).recordModeEvent("GOOGLE_API", "QWEN_LOCAL", 7, ACTOR, "DISPATCH_FENCED");
        order.verify(transactionManager).commit(any());
    }

    @Test
    void lostAcknowledgementCompareAndSetDoesNotFenceJobsOrRecordCompletion() {
        transactions();
        var entity = current(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.QWEN_LOCAL, "SWITCHING"));
        when(repository.tryAcquireExclusiveLock(DISPATCH_LOCK)).thenReturn(true);
        when(repository.findCurrentForUpdate()).thenReturn(Optional.of(entity));
        when(indexes.findByProviderAndStatusAndActivatedAtIsNotNull(AiProvider.LM_STUDIO, KnowledgeIndexVersionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        adapter.advanceSwitch();
        verifyNoInteractions(jobs);
        verify(repository, never()).recordModeEvent(anyString(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void publicationParticipatesInCallerTransactionAndRejectsWrongEpoch() {
        transactions();
        var entity = new AiRuntimeModeEntity();
        when(repository.findCurrentForPublication()).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(state(AiRuntimeMode.GOOGLE_API, AiRuntimeMode.GOOGLE_API, "READY"));
        assertThrows(AiRuntimeModeChangedException.class, () -> adapter.lockPublication(6));
        verify(transactionManager).getTransaction(argThat(definition -> definition.getPropagationBehavior()
                == TransactionDefinition.PROPAGATION_MANDATORY));
        verify(transactionManager).rollback(any());
    }

    @Test
    void telemetryIsWrittenThroughRepositoryWithOriginalEpochAndTimestamp() {
        transactions();
        var started = Instant.parse("2026-11-25T00:00:00Z");
        adapter.recordHttpCall(7, AiProvider.LM_STUDIO, "EMBEDDING", started, 30, 500, "NETWORK_ERROR");
        verify(repository).recordHttpCall(7, "LM_STUDIO", "EMBEDDING", started, 30, 500, "NETWORK_ERROR");
        verify(transactionManager).commit(any());
    }
}
