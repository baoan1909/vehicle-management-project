package com.ban.vehicle_management.application.ai.command;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;

/** Upload outcome so the API can distinguish a fresh upload (201) from a duplicate (200). */
public record KnowledgeDocumentUploadResult(KnowledgeDocument document, boolean created) {

    public static KnowledgeDocumentUploadResult created(KnowledgeDocument document) {
        return new KnowledgeDocumentUploadResult(document, true);
    }

    public static KnowledgeDocumentUploadResult duplicate(KnowledgeDocument document) {
        return new KnowledgeDocumentUploadResult(document, false);
    }
}