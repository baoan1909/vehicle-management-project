package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;

public interface KnowledgeRetrievalPortIn {

    /** Retrieval for the currently authenticated user, honoring their access scope. */
    KnowledgeRetrievalResult searchForCurrentUser(String query, int limit);
}