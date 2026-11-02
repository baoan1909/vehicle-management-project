package com.ban.vehicle_management.entrypoint.dto.iam.organization.request;

import java.time.Instant;
import java.util.UUID;

public record ReplaceParkingLotManagerRequest(
        UUID outgoingManagerId,
        UUID incomingManagerId,
        Instant effectiveAt,
        boolean acceptanceRequired,
        String handoverNote
) {}
