package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiRuntimeModePortOut;
import com.ban.vehicle_management.application.ai.service.AiLocalBusyException;
import com.ban.vehicle_management.application.ai.service.AiRuntimeModeChangedException;
import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiRuntimeModePersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiRuntimeModeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AssistantJobRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeIndexVersionRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class AiRuntimeModePersistenceAdapter implements AiRuntimeModePortOut {
    private static final long LOCAL_INFERENCE_LOCK = 641905241202L;
    private static final long DISPATCH_LOCK = 641905241201L;

    private final AiRuntimeModeRepository repository;
    private final AssistantJobRepository jobs;
    private final KnowledgeIndexVersionRepository indexes;
    private final AiRuntimeModePersistenceMapper mapper;
    private final TransactionTemplate isolated;
    private final TransactionTemplate publication;

    public AiRuntimeModePersistenceAdapter(AiRuntimeModeRepository repository,
            AssistantJobRepository jobs, KnowledgeIndexVersionRepository indexes,
            AiRuntimeModePersistenceMapper mapper, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.jobs = jobs;
        this.indexes = indexes;
        this.mapper = mapper;
        isolated = new TransactionTemplate(transactionManager);
        isolated.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        isolated.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        publication = new TransactionTemplate(transactionManager);
        publication.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }

    @Override
    public AiRuntimeState current() {
        return isolated.execute(status -> readState());
    }

    private AiRuntimeState readState() {
        return mapper.toDomain(repository.findCurrent()
                .orElseThrow(() -> new IllegalStateException("AI runtime mode is not initialized")));
    }

    @Override
    public AiRuntimeState requestSwitch(AiRuntimeMode target, long expectedEpoch, UUID actor) {
        if (target == null) throw new BadRequestException("Chọn chế độ AI hợp lệ");
        return isolated.execute(status -> {
            AiRuntimeState old = readState();
            if (old.epoch() != expectedEpoch || old.switching()) {
                throw new BadRequestException("Cấu hình đã thay đổi hoặc đang chuyển. Vui lòng tải lại.");
            }
            if (old.mode() == target) return old;
            if (repository.requestSwitch(target.name(), expectedEpoch, actor) != 1) {
                throw new BadRequestException("Cấu hình đã thay đổi. Vui lòng tải lại.");
            }
            repository.recordModeEvent(old.mode().name(), target.name(), expectedEpoch + 1, actor, "REQUESTED");
            return readState();
        });
    }

    @Override
    public boolean jobMatches(UUID jobId, long epoch) {
        return Boolean.TRUE.equals(isolated.execute(status -> jobs.existsByJobIdAndRuntimeEpoch(jobId, epoch)));
    }

    @Override
    public void lockPublication(long epoch) {
        // The row lock must live until the caller's message/action transaction commits.
        publication.executeWithoutResult(status -> {
            AiRuntimeState state = mapper.toDomain(repository.findCurrentForPublication()
                    .orElseThrow(() -> new IllegalStateException("AI runtime mode is not initialized")));
            if (state.epoch() != epoch || state.switching()) throw new AiRuntimeModeChangedException();
        });
    }

    @Override
    public void recordHttpCall(long epoch, AiProvider provider, String operation, Instant startedAt,
            long latencyMs, int status, String failure) {
        isolated.executeWithoutResult(transaction -> repository.recordHttpCall(
                epoch, provider.name(), operation, startedAt, latencyMs, status, failure));
    }

    @Override
    public ExecutionLease acquire(AiProvider provider, long expectedEpoch) {
        validate(current(), provider, expectedEpoch);
        var acquired = new CompletableFuture<AiRuntimeState>();
        var finished = new CompletableFuture<Void>();
        var released = new CountDownLatch(1);

        // A dedicated transaction holds the database locks throughout HTTP dispatch.
        // Its virtual thread keeps the caller's JPA transaction and switch requests independent.
        Thread.ofVirtual().name("ai-dispatch-lease").start(() -> {
            try {
                isolated.executeWithoutResult(status -> {
                    if (!repository.tryAcquireSharedLock(DISPATCH_LOCK)) throw new AiRuntimeModeChangedException();
                    if (provider == AiProvider.LM_STUDIO
                            && !repository.tryAcquireExclusiveLock(LOCAL_INFERENCE_LOCK)) {
                        throw new AiLocalBusyException();
                    }
                    AiRuntimeState state = readState();
                    validate(state, provider, expectedEpoch);
                    acquired.complete(state);
                    try {
                        released.await();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("AI dispatch lease interrupted", interrupted);
                    }
                });
                finished.complete(null);
            } catch (RuntimeException | Error failure) {
                acquired.completeExceptionally(failure);
                finished.completeExceptionally(failure);
            }
        });

        try {
            AiRuntimeState state = await(acquired);
            return new ExecutionLease() {
                private final AtomicBoolean closed = new AtomicBoolean();

                @Override
                public long epoch() { return state.epoch(); }

                @Override
                public void close() {
                    if (closed.compareAndSet(false, true)) {
                        released.countDown();
                        await(finished);
                    }
                }
            };
        } catch (RuntimeException failure) {
            released.countDown();
            finished.handle((ignored, transactionFailure) -> null).join();
            if (failure instanceof AiRuntimeModeChangedException || failure instanceof AiLocalBusyException) throw failure;
            throw new IllegalStateException("AI dispatch lease unavailable", failure);
        }
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException failure) {
            if (failure.getCause() instanceof RuntimeException cause) throw cause;
            if (failure.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException("AI dispatch transaction failed", failure.getCause());
        }
    }

    private void validate(AiRuntimeState state, AiProvider provider, long epoch) {
        if (state.switching() || state.mode().provider() != provider || (epoch >= 0 && epoch != state.epoch())) {
            throw new AiRuntimeModeChangedException();
        }
    }

    @Override
    public void advanceSwitch() {
        AiRuntimeState before = current();
        if (!before.switching() && !"PREPARING".equals(before.status())) return;
        isolated.executeWithoutResult(status -> {
            if (!repository.tryAcquireExclusiveLock(DISPATCH_LOCK)) return;
            // First entity read in this transaction; serialize acknowledgement with mode updates.
            AiRuntimeState latest = mapper.toDomain(repository.findCurrentForUpdate()
                    .orElseThrow(() -> new IllegalStateException("AI runtime mode is not initialized")));
            if (!latest.switching() && !"PREPARING".equals(latest.status())) return;
            boolean ready = indexes.findByProviderAndStatusAndActivatedAtIsNotNull(
                    latest.targetMode().provider(), KnowledgeIndexVersionStatus.ACTIVE).isPresent();
            if (latest.switching()) {
                if (repository.completeSwitch(latest.epoch(), ready ? "READY" : "PREPARING") == 1) {
                    jobs.failJobsFromOtherRuntimeEpochs(latest.epoch());
                    repository.recordModeEvent(latest.mode().name(), latest.targetMode().name(),
                            latest.epoch(), latest.changedBy(), "DISPATCH_FENCED");
                }
            } else if (ready) {
                repository.markReady(latest.epoch());
            }
        });
    }
}
