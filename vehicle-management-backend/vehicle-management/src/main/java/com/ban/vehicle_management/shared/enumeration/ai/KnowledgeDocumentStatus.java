package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Lifecycle of an uploaded document. Publish moves REVIEW/DRAFT documents to READY
 * together with their chunks; archive hides the document and yields a new candidate
 * index.
 */
public enum KnowledgeDocumentStatus {
    PENDING,
    PROCESSING,
    REVIEW,
    READY,
    FAILED,
    ARCHIVED
}