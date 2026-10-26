package com.ban.vehicle_management.infrastructure.security.assistant;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssistantActorScopeTest {
    private final AssistantActorScope scope = new AssistantActorScope();

    @Test
    void actorIsAvailableOnlyInsideJobAndCleanedAfterSuccess() {
        UUID actorId = UUID.randomUUID();
        assertTrue(scope.currentActorAccountId().isEmpty());
        assertEquals(actorId, scope.runAs(actorId, () -> scope.currentActorAccountId().orElseThrow()));
        assertTrue(scope.currentActorAccountId().isEmpty());
    }

    @Test
    void exceptionCannotLeakActorIntoNextJob() {
        UUID firstActor = UUID.randomUUID();
        assertThrows(IllegalStateException.class,
                () -> scope.runAs(firstActor, () -> { throw new IllegalStateException("boom"); }));
        assertTrue(scope.currentActorAccountId().isEmpty());

        UUID nextActor = UUID.randomUUID();
        assertEquals(nextActor,
                scope.runAs(nextActor, () -> scope.currentActorAccountId().orElseThrow()));
        assertTrue(scope.currentActorAccountId().isEmpty());
    }

    @Test
    void nestedScopeRestoresPreviousActor() {
        UUID outer = UUID.randomUUID();
        UUID inner = UUID.randomUUID();
        scope.runAs(outer, () -> {
            assertEquals(inner,
                    scope.runAs(inner, () -> scope.currentActorAccountId().orElseThrow()));
            assertEquals(outer, scope.currentActorAccountId().orElseThrow());
        });
        assertTrue(scope.currentActorAccountId().isEmpty());
    }
}
