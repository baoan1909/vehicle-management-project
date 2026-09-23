package com.ban.vehicle_management.domain.ai.model;

/**
 * A single model function-call turn. Provider call identifiers and thought
 * signatures are preserved when the provider supplies them so multi-turn
 * tool loops can be replayed in protocol order.
 */
public record AiFunctionCall(
        String name,
        String argumentsJson,
        boolean malformed,
        String failureCode,
        String providerCallId,
        String thoughtSignature
) {
    public AiFunctionCall(String name, String argumentsJson, boolean malformed, String failureCode) {
        this(name, argumentsJson, malformed, failureCode, null, null);
    }
}
