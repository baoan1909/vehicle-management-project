package com.ban.vehicle_management.domain.ai.model;

/**
 * Backend function-response turn answering one {@link AiFunctionCall}. It must
 * always be preceded by its model function-call turn in the request payload,
 * followed by protocol sequence order.
 */
public record AiFunctionResponse(
        String name,
        String responseJson,
        String providerCallId
) {
    public AiFunctionResponse(String name, String responseJson) {
        this(name, responseJson, null);
    }
}
