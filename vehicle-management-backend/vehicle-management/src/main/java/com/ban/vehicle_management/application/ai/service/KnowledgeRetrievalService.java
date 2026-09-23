package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.in.KnowledgeRetrievalPortIn;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.GroundedConfidenceCalculator;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.domain.ai.model.VietnameseQueryNormalizer;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Knowledge retrieval entry point used by the assistant RAG tool and by the admin
 * retrieval-test screen.
 *
 * <p>Flow: normalize query → resolve authenticated access context → resolve ACTIVE
 * index → embed query → single-statement hybrid search (vector + lexical, weighted
 * RRF, per-document cap) → grounded confidence → evidence threshold → audit.</p>
 *
 * <p>A null tenant is deliberately restricted to global chunks. A non-null
 * tenant may see global chunks plus chunks owned by that exact tenant, provided
 * TENANT_PRIVATE is present in the server-resolved scope allowlist.</p>
 */
@Service
public class KnowledgeRetrievalService implements KnowledgeRetrievalPortIn {

    public static final String DIAG_FEATURE_DISABLED = "FEATURE_DISABLED";
    public static final String DIAG_NO_ACTIVE_KNOWLEDGE_INDEX = "NO_ACTIVE_KNOWLEDGE_INDEX";
    public static final String DIAG_INDEX_CONFIG_INVALID = "INDEX_CONFIG_INVALID";
    public static final String DIAG_EMBEDDING_FAILED = "EMBEDDING_FAILED";
    public static final String DIAG_RETRIEVAL_FAILED = "RETRIEVAL_FAILED";
    public static final String DIAG_INSUFFICIENT_EVIDENCE = "INSUFFICIENT_EVIDENCE";

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalService.class);

    private final KnowledgeRetrievalPortOut retrievalPortOut;
    private final KnowledgeIndexVersionPortOut indexVersionPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final KnowledgeRetrievalAuditPortOut auditPortOut;
    private final EmbeddingService embeddingService;
    private final EmbeddingPromptFormatter promptFormatter;
    private final PiiRedactionService piiRedactionService;
    private final EmbeddingProperties properties;
    private final RetrievalProperties retrievalProperties;
    private final KnowledgeAccessContextResolver accessContextResolver;

    public KnowledgeRetrievalService(
            KnowledgeRetrievalPortOut retrievalPortOut,
            KnowledgeIndexVersionPortOut indexVersionPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            KnowledgeRetrievalAuditPortOut auditPortOut,
            EmbeddingService embeddingService,
            EmbeddingPromptFormatter promptFormatter,
            PiiRedactionService piiRedactionService,
            EmbeddingProperties properties,
            RetrievalProperties retrievalProperties,
            KnowledgeAccessContextResolver accessContextResolver
    ) {
        this.retrievalPortOut = retrievalPortOut;
        this.indexVersionPortOut = indexVersionPortOut;
        this.configurationPortOut = configurationPortOut;
        this.auditPortOut = auditPortOut;
        this.embeddingService = embeddingService;
        this.promptFormatter = promptFormatter;
        this.piiRedactionService = piiRedactionService;
        this.properties = properties;
        this.retrievalProperties = retrievalProperties;
        this.accessContextResolver = accessContextResolver;
    }

    @Override
    public KnowledgeRetrievalResult searchForCurrentUser(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return search(KnowledgeRetrievalContext.global(), query, accessContextResolver.resolveScopes(), safeLimit);
    }

    public KnowledgeRetrievalResult search(
            KnowledgeRetrievalContext retrievalContext,
            String query,
            List<String> accessScopes,
            int limit) {
        Instant startedAt = Instant.now();
        KnowledgeRetrievalContext context = retrievalContext == null
                ? KnowledgeRetrievalContext.global()
                : retrievalContext;
        if (query == null || query.isBlank() || !properties.isEnabled()) {
            return auditedEmpty(context, null, query, accessScopes, limit, startedAt, DIAG_FEATURE_DISABLED);
        }
        List<String> scopes = accessScopes == null ? List.of() : List.copyOf(accessScopes);
        if (scopes.contains(KnowledgeAccessScope.TENANT_PRIVATE.name()) && context.tenantId() == null) {
            throw new BadRequestException("TENANT_CONTEXT_REQUIRED");
        }
        String redactedQuery = piiRedactionService.redact(query).value();
        VietnameseQueryNormalizer.NormalizedQuery normalized;
        try {
            normalized = VietnameseQueryNormalizer.normalize(redactedQuery);
        } catch (IllegalArgumentException exception) {
            return auditedEmpty(context, null, redactedQuery, scopes, limit, startedAt, DIAG_FEATURE_DISABLED);
        }
        KnowledgeIndexVersion active = indexVersionPortOut.findActive().orElse(null);
        if (active == null) {
            return auditedEmpty(context, null, redactedQuery, scopes, limit, startedAt,
                    DIAG_NO_ACTIVE_KNOWLEDGE_INDEX);
        }
        AiModelConfiguration configuration = findUsableEmbeddingConfiguration(active);
        if (configuration == null || !promptFormatter.supports(active.getEmbeddingPromptVersion())) {
            return auditedEmpty(context, active.getIndexVersionId(), redactedQuery, scopes, limit,
                    startedAt, DIAG_INDEX_CONFIG_INVALID);
        }
        EmbeddingResult result;
        try {
            String formatted = promptFormatter.formatQuery(active.getEmbeddingPromptVersion(), redactedQuery);
            result = embeddingService.embed(new EmbeddingRequest(formatted, null), configuration);
            if (!result.isSuccess() || result.getVector() == null
                    || result.getVector().dimension() != active.getDimension()) {
                return auditedEmpty(context, active.getIndexVersionId(), redactedQuery, scopes, limit,
                        startedAt, DIAG_EMBEDDING_FAILED);
            }
        } catch (Exception exception) {
            log.warn("Query embedding failed", exception);
            return auditedEmpty(context, active.getIndexVersionId(), redactedQuery, scopes, limit,
                    startedAt, DIAG_EMBEDDING_FAILED);
        }
        try {
            int effectiveTopK = Math.min(Math.max(1, limit), retrievalProperties.getFinalTopK());
            List<HybridSearchRow> rows = retrievalPortOut.hybridSearch(
                    context.tenantId(),
                    active.getIndexVersionId(),
                    result.getVector(),
                    normalized.normalized(),
                    scopes,
                    retrievalProperties.getVectorCandidateLimit(),
                    retrievalProperties.getLexicalCandidateLimit(),
                    retrievalProperties.getMinimumVectorScore(),
                    retrievalProperties.getMinimumLexicalScore(),
                    retrievalProperties.getRrfRankConstant(),
                    retrievalProperties.getWeightVector(),
                    retrievalProperties.getWeightLexical(),
                    retrievalProperties.getMaxChunksPerDocument(),
                    effectiveTopK);
            List<KnowledgeSearchResult> fused = rows.stream()
                    .map(row -> new KnowledgeSearchResult(
                            row.documentId(), row.chunkId(), row.title(), row.content(), row.summary(),
                            row.sourcePage(), row.sourceSection(), row.fusedScore()))
                    .toList();
            double confidence = groundedConfidence(rows, fused.size(), effectiveTopK);
            boolean sufficient = !fused.isEmpty()
                    && GroundedConfidenceCalculator.decide(confidence,
                            retrievalProperties.getGroundedConfidenceThreshold(),
                            retrievalProperties.getCautionConfidenceThreshold())
                    != GroundedConfidenceCalculator.EvidenceDecision.INSUFFICIENT;
            String diagnostic = sufficient ? null : DIAG_INSUFFICIENT_EVIDENCE;
            RetrievalAudit audit = persistAudit(context, redactedQuery, normalized.normalized(),
                    active.getIndexVersionId(), scopes, rows, fused.size(), effectiveTopK,
                    confidence, diagnostic, startedAt);
            return new KnowledgeRetrievalResult(diagnostic, active.getIndexVersionId(), fused,
                    audit.retrievalAuditId(),
                    BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP),
                    sufficient, normalized.normalized(), retrievalProperties.getPolicyVersion());
        } catch (Exception exception) {
            log.warn("Hybrid retrieval failed", exception);
            return auditedEmpty(context, active.getIndexVersionId(), redactedQuery, scopes, limit,
                    startedAt, DIAG_RETRIEVAL_FAILED);
        }
    }

    private double groundedConfidence(List<HybridSearchRow> rows, int finalCount, int limit) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        double topVector = rows.stream()
                .filter(row -> row.vectorScore() != null)
                .mapToDouble(row -> row.vectorScore().doubleValue())
                .max().orElse(0.0);
        double topLexical = rows.stream()
                .filter(row -> row.lexicalScore() != null)
                .mapToDouble(row -> Math.min(1.0, row.lexicalScore().doubleValue()))
                .max().orElse(0.0);
        long both = rows.stream()
                .filter(row -> row.vectorRank() != null && row.lexicalRank() != null)
                .count();
        double agreement = (double) both / (double) rows.size();
        double coverage = Math.min(1.0, (double) finalCount / (double) Math.max(1, Math.min(limit, 50)));
        return GroundedConfidenceCalculator.confidence(new GroundedConfidenceCalculator.EvidenceSignals(
                topVector, topLexical, agreement, coverage, 1.0, true, finalCount));
    }

    private KnowledgeRetrievalResult auditedEmpty(
            KnowledgeRetrievalContext context, UUID activeIndexVersionId, String rawQuery,
            List<String> scopes, int limit, Instant startedAt, String diagnostic) {
        List<String> safeScopes = scopes == null ? List.of() : List.copyOf(scopes);
        String normalizedQuery = null;
        if (rawQuery != null && !rawQuery.isBlank()) {
            try {
                normalizedQuery = VietnameseQueryNormalizer.normalize(rawQuery).normalized();
            } catch (IllegalArgumentException ignored) {
                normalizedQuery = null;
            }
        }
        RetrievalAudit audit = persistAudit(context, rawQuery, normalizedQuery,
                activeIndexVersionId, safeScopes, List.of(), 0, limit, 0.0, diagnostic, startedAt);
        return new KnowledgeRetrievalResult(diagnostic, activeIndexVersionId, List.of(),
                audit.retrievalAuditId(), BigDecimal.ZERO, false, normalizedQuery,
                retrievalProperties.getPolicyVersion());
    }

    private RetrievalAudit persistAudit(
            KnowledgeRetrievalContext context,
            String rawQuery, String normalizedQuery, UUID activeIndexVersionId, List<String> scopes,
            List<HybridSearchRow> rows, int finalCount, int limit, double confidence,
            String diagnostic, Instant startedAt) {
        Instant now = Instant.now();
        String redacted = "";
        try {
            redacted = piiRedactionService.redact(rawQuery == null ? "" : rawQuery).value();
        } catch (Exception ignored) {
            redacted = "";
        }
        List<RetrievalAudit.RetrievalAuditResult> results = rows.stream()
                .map(row -> new RetrievalAudit.RetrievalAuditResult(
                        row.documentId(), row.chunkId(), row.vectorRank(), row.lexicalRank(),
                        row.vectorScore(), row.lexicalScore(), row.fusedScore(), row.finalRank(), false))
                .toList();
        RetrievalAudit audit = new RetrievalAudit(
                UUID.randomUUID(), context.runId(), context.conversationId(), context.inputMessageId(), null,
                context.requestedBy(), context.tenantId(),
                redacted.length() > 2000 ? redacted.substring(0, 2000) : redacted,
                normalizedQuery == null ? null : sha256Hex(normalizedQuery),
                activeIndexVersionId, retrievalProperties.getPolicyVersion(),
                retrievalProperties.getThresholdVersion(),
                String.join(",", scopes),
                (int) rows.stream().filter(row -> row.vectorRank() != null).count(),
                (int) rows.stream().filter(row -> row.lexicalRank() != null).count(),
                rows.size(), finalCount, limit, retrievalProperties.getMaxChunksPerDocument(),
                BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP),
                diagnostic,
                Math.max(0, now.toEpochMilli() - startedAt.toEpochMilli()),
                now, results);
        return auditPortOut.save(audit);
    }

    private AiModelConfiguration findUsableEmbeddingConfiguration(KnowledgeIndexVersion active) {
        AiModelConfiguration configuration = configurationPortOut.findById(active.getModelConfigurationId()).orElse(null);
        if (configuration == null
                || configuration.getUseCase() != AiUseCase.EMBEDDING
                || configuration.getOutputDimension() == null
                || configuration.getOutputDimension() != active.getDimension()
                || configuration.getStatus() == AiModelStatus.DISABLED
                || configuration.getProvider() != active.getProvider()
                || !configuration.getModelId().equals(active.getModelId())) {
            return null;
        }
        return configuration;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
