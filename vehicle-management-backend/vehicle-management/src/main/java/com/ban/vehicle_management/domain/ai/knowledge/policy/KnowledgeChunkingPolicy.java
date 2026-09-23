package com.ban.vehicle_management.domain.ai.knowledge.policy;

/**
 * Chunk sizing and token-estimation rules for the knowledge ingestion pipeline.
 * Splitting favors semantic boundaries (headings/list items) and enforces hard upper
 * and lower bounds so embeddings yields consistent, useful vectors.
 */
public final class KnowledgeChunkingPolicy {

    private KnowledgeChunkingPolicy() {
    }

    public static final int CHUNK_TARGET_TOKENS = 500;
    public static final int CHUNK_MAX_TOKENS = 800;
    public static final int CHUNK_MIN_TOKENS = 50;
    public static final int CHUNK_OVERLAP_TOKENS = 75;
    public static final int MAX_CHUNK_CONTENT_CHARS = 6000;

    /** Deterministic token estimate used for sizing only; roughly one token = 4 chars. */
    public static long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        long words = text.split("\\s+").length;
        long chars = text.length();
        return Math.max(1, Math.max(words, chars / 4L));
    }

    public static boolean exceedsMax(long tokens) {
        return tokens > CHUNK_MAX_TOKENS;
    }
}