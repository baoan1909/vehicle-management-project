package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Sub-stage of a PROCESSING ingestion job. EXTRACTING -> CHUNKING -> EMBEDDING -> REVIEW.
 */
public enum IngestionStage {
    EXTRACTING,
    CHUNKING,
    EMBEDDING,
    REVIEW;

    public IngestionStage next() {
        return switch (this) {
            case EXTRACTING -> CHUNKING;
            case CHUNKING -> EMBEDDING;
            case EMBEDDING -> REVIEW;
            case REVIEW -> REVIEW;
        };
    }
}