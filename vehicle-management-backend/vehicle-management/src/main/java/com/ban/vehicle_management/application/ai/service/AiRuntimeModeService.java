package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.AiRuntimeModePortIn;
import com.ban.vehicle_management.application.ai.port.out.AiRuntimeModePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiRuntimeState;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import org.springframework.security.access.AccessDeniedException;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AiRuntimeModeService implements AiRuntimeModePortIn {
    private final AiRuntimeModePortOut store;
    private final CurrentAccountPortIn accounts;
    private final ThreadLocal<Long> scopedEpoch = new ThreadLocal<>();

    public AiRuntimeModeService(AiRuntimeModePortOut store, CurrentAccountPortIn accounts) {
        this.store = store;
        this.accounts = accounts;
    }

    public AiRuntimeState current() { return store.current(); }
    public AiProvider provider() { return current().mode().provider(); }
    public boolean allows(AiProvider provider) {
        var state = current();
        return !state.switching() && state.mode().provider() == provider;
    }
    public boolean ready() { return current().ready(); }
    public boolean callsAllowed(AiAssistantProperties properties) {
        // Preserve the existing support-data opt-in and approved-model policy in both modes.
        // Local selection removes dependence on Google keys/network, not data permissions.
        return properties.isProviderCallAllowed();
    }
    public void publicationFence() {
        store.lockPublication(scopedEpoch.get() == null ? current().epoch() : scopedEpoch.get());
    }
    public void recordHttpCall(long epoch, AiProvider provider, String operation, java.time.Instant startedAt,
            long latencyMs, int status, String failure) {
        store.recordHttpCall(epoch, provider, operation, startedAt, latencyMs, status, failure);
    }
    public boolean isCurrentScope() {
        var state = current();
        return !state.switching() && (scopedEpoch.get() == null || state.epoch() == scopedEpoch.get());
    }
    public void assertCurrent() {
        var state = current();
        if (state.switching() || (scopedEpoch.get() != null && state.epoch() != scopedEpoch.get())) {
            throw new AiRuntimeModeChangedException();
        }
    }
    public Scope openScope() {
        var state = current();
        if (state.switching()) throw new AiRuntimeModeChangedException();
        Long previous = scopedEpoch.get();
        scopedEpoch.set(state.epoch());
        return () -> { if (previous == null) scopedEpoch.remove(); else scopedEpoch.set(previous); };
    }
    public boolean jobMatches(UUID jobId) { return store.jobMatches(jobId, current().epoch()); }
    public AiRuntimeModePortOut.ExecutionLease acquire(AiProvider provider) {
        return store.acquire(provider, scopedEpoch.get() == null ? -1 : scopedEpoch.get());
    }
    @Override public AiRuntimeState state() {
        accounts.requirePermission("AI_MODEL_READ_ALL");
        return current();
    }
    @Override public AiRuntimeState changeMode(AiRuntimeMode mode, long expectedEpoch) {
        accounts.requirePermission("AI_MODEL_MANAGE_ALL");
        var actor = accounts.getCurrentAccountOrThrow();
        if (!"system_admin".equalsIgnoreCase(actor.roleCode())) {
            throw new AccessDeniedException("Chỉ quản trị hệ thống được chuyển chế độ AI");
        }
        return store.requestSwitch(mode, expectedEpoch, actor.accountId());
    }
    @Scheduled(fixedDelayString = "${app.ai.runtime-switch-delay-ms:500}", initialDelayString = "${app.ai.runtime-switch-initial-delay-ms:5000}")
    public void advanceSwitch() { store.advanceSwitch(); }

    public interface Scope extends AutoCloseable { @Override void close(); }
}
