package com.ban.vehicle_management.infrastructure.security.assistant;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Scheduler-safe actor propagation for the assistant worker. HTTP requests
 * resolve the actor from the {@code SecurityContext}; scheduler threads have
 * none, so the orchestrator binds the persisted {@code requestedBy} account
 * (from the input message sender) for the duration of one job.
 *
 * <p>The actor id always comes from persisted job data, never from model
 * output, request parameters or frontend input.</p>
 */
@Component
public class AssistantActorScope {

    private final ThreadLocal<UUID> actorAccountId = new ThreadLocal<>();

    public Optional<UUID> currentActorAccountId() {
        return Optional.ofNullable(actorAccountId.get());
    }

    public <T> T runAs(UUID accountId, Supplier<T> work) {
        UUID previous = actorAccountId.get();
        actorAccountId.set(accountId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                actorAccountId.remove();
            } else {
                actorAccountId.set(previous);
            }
        }
    }

    public void runAs(UUID accountId, Runnable work) {
        runAs(accountId, () -> {
            work.run();
            return null;
        });
    }
}
