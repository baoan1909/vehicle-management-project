package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Lifecycle of an ingestion job; the extraction/embedding progress is tracked by
 * {@link IngestionStage}. A job in REVIEW has finished extracting, chunking and
 * staging embeddings for its document and waits for the reviewer.
 */
public enum KnowledgeIngestionJobStatus {
    PENDING,
    PROCESSING,
    REVIEW,
    READY,
    RETRYING,
    FAILED
}