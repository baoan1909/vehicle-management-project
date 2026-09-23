package com.ban.vehicle_management.domain.ai.model;

/**
 * Backend-decided conversation intent. The router runs before generation and
 * selects exactly one branch; the model never overrides it.
 */
public enum AssistantIntent {
    GREETING,
    STATIC_KNOWLEDGE,
    PERSONAL_DATA,
    WRITE_ACTION,
    OUT_OF_SCOPE,
    SECURITY_REFUSAL,
    HANDOFF_REQUEST
}
