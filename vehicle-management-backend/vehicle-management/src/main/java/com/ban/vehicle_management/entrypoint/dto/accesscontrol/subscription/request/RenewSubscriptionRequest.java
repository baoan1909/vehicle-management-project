package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RenewSubscriptionRequest(
        @NotNull UUID ticketTypeId,
        @NotNull @Min(1) @Max(12) Integer periodQuantity,
        String voucherCode,
        Boolean physicalCardRequested
) {
}
