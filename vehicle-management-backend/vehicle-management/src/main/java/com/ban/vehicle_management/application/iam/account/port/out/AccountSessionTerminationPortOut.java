package com.ban.vehicle_management.application.iam.account.port.out;

import java.time.Instant;
import java.util.UUID;

/** Publishes an ephemeral signal that forces every active client session to leave the application. */
public interface AccountSessionTerminationPortOut {

    void publish(UUID accountId, UUID deletionRequestId, Instant occurredAt);
}
