package com.ban.vehicle_management.application.ai.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hybrid retrieval policy. All candidate-pool, RRF, per-document and threshold
 * values live here (or in an equivalent database configuration) so behaviour
 * can be tuned without code changes.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai.retrieval")
public class RetrievalProperties {

    /** Final number of fused results returned to callers. */
    private int finalTopK = 5;

    /** Candidates pulled from the vector branch before fusion. */
    private int vectorCandidateLimit = 20;

    /** Candidates pulled from the lexical branch before fusion. */
    private int lexicalCandidateLimit = 20;

    /** Maximum chunks taken from a single document into the fused set. */
    private int maxChunksPerDocument = 3;

    /** RRF rank constant (k). */
    private int rrfRankConstant = 60;

    /** RRF branch weights; they must sum to 1. */
    private double weightVector = 0.6;

    /** RRF branch weights; they must sum to 1. */
    private double weightLexical = 0.4;

    /** Minimum cosine similarity for a vector candidate to be considered. */
    private double minimumVectorScore = 0.15;

    /** Minimum ts_rank for a lexical candidate to be considered. */
    private double minimumLexicalScore = 0.01;

    /**
     * Grounded confidence (0-1, computed — never raw RRF) at or above which a
     * grounded answer may be produced.
     */
    private double groundedConfidenceThreshold = 0.80;

    /** Confidence below grounded threshold but at/above this yields a cautious answer + handoff. */
    private double cautionConfidenceThreshold = 0.55;

    /** Policy version recorded on every retrieval audit. */
    private String policyVersion = "retrieval-policy-v1";

    /** Threshold-set version recorded on every retrieval audit. */
    private String thresholdVersion = "retrieval-threshold-v1";

    /** Fail startup when the {@code unaccent} extension is unavailable (production). */
    private boolean unaccentRequired = false;

    /** Maximum age of a cached query embedding reuse, if enabled by callers. */
    private Duration queryEmbeddingTtl = Duration.ofMinutes(10);
}
