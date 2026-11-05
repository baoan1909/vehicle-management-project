package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectSubscriptionSpaceRequest(
        @NotNull UUID parkingSpaceId
) {
}
