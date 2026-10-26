package com.ban.vehicle_management.infrastructure.realtime.security;

import com.ban.vehicle_management.domain.iam.account.model.CurrentAccountAccess;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/** Immutable actor snapshot owned by exactly one STOMP session. */
public record WebSocketActorPrincipal(
        UUID accountId,
        CurrentAccountAccess accessSnapshot,
        String accessRevision
) implements Principal {

    public WebSocketActorPrincipal {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(accessSnapshot, "accessSnapshot");
        Objects.requireNonNull(accessRevision, "accessRevision");
        if (!accountId.equals(accessSnapshot.accountId())) {
            throw new IllegalArgumentException("WebSocket actor snapshot account mismatch");
        }
    }

    @Override
    public String getName() {
        return accountId.toString();
    }
}
