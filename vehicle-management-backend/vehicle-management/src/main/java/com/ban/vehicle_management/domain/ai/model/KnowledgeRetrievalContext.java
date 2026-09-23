package com.ban.vehicle_management.domain.ai.model;

import java.util.UUID;

/**
 * Trace and isolation context for one retrieval operation.
 *
 * <p>The context is supplied by the application layer, never by the model. A
 * null tenant means global knowledge only; a non-null tenant permits global
 * knowledge plus knowledge owned by that exact tenant.</p>
 */
public record KnowledgeRetrievalContext(
        UUID runId,
        UUID conversationId,
        UUID inputMessageId,
        UUID requestedBy,
        UUID tenantId
) {

    public static KnowledgeRetrievalContext global() {
        return new KnowledgeRetrievalContext(null, null, null, null, null);
    }

    public static KnowledgeRetrievalContext forTenant(UUID tenantId) {
        return new KnowledgeRetrievalContext(null, null, null, null, tenantId);
    }
}
