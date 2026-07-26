package com.ban.vehicle_management.entrypoint.dto.billing.payment.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnpayIpnResponse(
        @JsonProperty("RspCode") String responseCode,
        @JsonProperty("Message") String message
) {
}
