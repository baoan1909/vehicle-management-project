package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import java.util.UUID;

public interface AiRuntimeModePortOut {
    AiRuntimeState current();
    AiRuntimeState requestSwitch(AiRuntimeMode target, long expectedEpoch, UUID actor);
    void advanceSwitch();
    boolean jobMatches(UUID jobId, long epoch);
    void lockPublication(long epoch);
    void recordHttpCall(long epoch, AiProvider provider, String operation, java.time.Instant startedAt,
            long latencyMs, int status, String failure);
    ExecutionLease acquire(AiProvider provider, long expectedEpoch);

    interface ExecutionLease extends AutoCloseable {
        default long epoch() { return -1; }
        @Override void close();
    }
}
