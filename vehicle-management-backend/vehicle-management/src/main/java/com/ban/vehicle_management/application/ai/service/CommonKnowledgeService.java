package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.CommonKnowledgePortOut;
import com.ban.vehicle_management.domain.ai.model.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** No provider, embedding, model configuration or generated-answer cache dependencies. */
@Service
public class CommonKnowledgeService {
    public static final String METHOD = "COMMON_FAQ";
    public static final String POLICY = "reviewed-faq-v2-blocks/" + CanonicalQuestionKey.VERSION;
    private static final int MAX_CANDIDATES = 32;
    private static final Pattern TERM = Pattern.compile("[\\p{L}\\p{N}_]{2,64}");
    private final CommonKnowledgePortOut knowledge;
    public CommonKnowledgeService(CommonKnowledgePortOut knowledge) { this.knowledge = knowledge; }

    public record Resolution(CommonKnowledgeCandidate source, String answer, String snapshot, boolean conflict) { }

    public Optional<Resolution> lookup(KnowledgeRetrievalContext context, String question, List<String> scopes) {
        if (scopes.isEmpty() || AssistantIntentRouter.route(question).intent() != AssistantIntent.STATIC_KNOWLEDGE) return Optional.empty();
        String signature = CanonicalQuestionKey.of(question);
        var matcher = TERM.matcher(signature.substring(signature.indexOf('|') + 1));
        Set<String> terms = new LinkedHashSet<>();
        while (matcher.find()) terms.add(matcher.group());
        if (terms.isEmpty() || terms.size() > 24) return Optional.empty();
        // Terms contain letters/numbers only; no caller-supplied tsquery operators.
        var candidates = knowledge.searchReviewedFaqs(context.tenantId(), scopes, String.join(" | ", terms), MAX_CANDIDATES + 1);
        // A bounded window must never hide a conflicting FAQ and still declare a unique answer.
        if (candidates.size() > MAX_CANDIDATES) return Optional.empty();
        var match = GroundedFaqLookup.findCommon(question, candidates.stream().map(CommonKnowledgeCandidate::evidence).toList());
        if (match.conflict()) return Optional.of(new Resolution(null, null, null, true));
        if (match.match() == null) return Optional.empty();
        var source = candidates.stream().filter(c -> c.chunkId().equals(match.match().chunkId())).findFirst().orElseThrow();
        return Optional.of(new Resolution(source, match.match().answer(), fingerprint(List.of(source)), false));
    }

    @Transactional
    public boolean verify(KnowledgeRetrievalContext context, List<String> scopes, KnowledgeRetrievalResult result) {
        if (!result.isCommonFaq()) return true;
        if (scopes.isEmpty() || result.results().isEmpty() || result.sourceSnapshot() == null) return false;
        var ids = result.results().stream().map(KnowledgeSearchResult::chunkId).distinct().toList();
        var current = knowledge.lockReviewedFaqs(context.tenantId(), scopes, ids);
        return current.size() == ids.size() && result.sourceSnapshot().equals(fingerprint(current));
    }

    public static String fingerprint(List<CommonKnowledgeCandidate> sources) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(POLICY.getBytes(StandardCharsets.UTF_8));
            for (var source : sources.stream().sorted(Comparator.comparing(c -> c.chunkId().toString())).toList()) {
                for (Object field : List.of(source.documentId(), source.chunkId(), source.documentVersion(),
                        source.reviewedAt(), Objects.toString(source.documentChecksum(), ""),
                        Objects.toString(source.title(), ""), Objects.toString(source.sourceSection(), ""), source.content())) {
                    byte[] bytes = field.toString().getBytes(StandardCharsets.UTF_8);
                    digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
                }
            }
            String versions = sources.stream().sorted(Comparator.comparing(c -> c.chunkId().toString()))
                    .map(c -> c.documentId() + "@" + c.documentVersion() + "/" + c.chunkId())
                    .collect(java.util.stream.Collectors.joining(","));
            return POLICY + ":" + versions + ":" + HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
