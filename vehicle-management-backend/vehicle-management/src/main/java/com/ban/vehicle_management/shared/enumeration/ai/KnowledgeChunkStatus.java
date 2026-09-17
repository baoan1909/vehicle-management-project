package com.ban.vehicle_management.shared.enumeration.ai;

/**
 * Chunks are DRAFT while their document waits for review, become READY on publish
 * and ARCHIVED when the owning document is archived.
 */
public enum KnowledgeChunkStatus {
    DRAFT,
    READY,
    ARCHIVED
}