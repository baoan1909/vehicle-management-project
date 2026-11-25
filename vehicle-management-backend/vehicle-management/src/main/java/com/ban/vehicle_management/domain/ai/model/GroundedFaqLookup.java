package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Verbatim guidance from FAQ evidence. Shared matching uses conservative framing/alias keys;
 * contact aliases require the named company and never answer troubleshooting or live-data questions. */
public final class GroundedFaqLookup {
    private static final Pattern QA_LINE = Pattern.compile(
            "(?m)^[ \\t]*(?:[-*][ \\t]+)?([^\\r\\n?]{3,499}\\?)[ \\t]+([^\\r\\n]+)$");

    public record Match(String answer, UUID chunkId) { }

    public record Lookup(Match match, boolean conflict) {
        public Optional<Match> answer() { return Optional.ofNullable(match); }
    }

    public static boolean isFaqSection(String section) {
        String folded = VietnameseQueryNormalizer.fold(section);
        int separator = Math.max(folded.lastIndexOf('>'),folded.lastIndexOf('/'));
        String heading = folded.substring(separator + 1).strip();
        return heading.equals("faq") || (heading.startsWith("cau hoi") && heading.endsWith("thuong gap"));
    }

    public static Lookup findCommon(String question, List<KnowledgeSearchResult> evidence) {
        return lookup(question, evidence, true);
    }

    private GroundedFaqLookup() { }

    public static Optional<Match> find(String question, List<KnowledgeSearchResult> evidence) {
        return lookup(question, evidence, false).answer();
    }

    private static Lookup lookup(String question, List<KnowledgeSearchResult> evidence, boolean common) {
        if (question == null || question.isBlank() || evidence == null
                || AssistantIntentRouter.route(question).intent() != AssistantIntent.STATIC_KNOWLEDGE) {
            return new Lookup(null, false);
        }
        String key = common ? CanonicalQuestionKey.of(question) : key(question);
        ContactKind contact = companyContactIntent(question);
        Match found = null;
        for (var source : evidence) {
            if (source.chunkId() == null || source.content() == null || (common && !isFaqSection(source.sourceSection()))) continue;
            // An overlap chunk can start in the previous section. Its first-section metadata
            // is not an exhaustive list of the content: require the complete Q/A line itself.
            if (PromptInjectionScreening.scan(source.content()).highRisk()) continue;
            var lines = QA_LINE.matcher(source.content());
            while (lines.find()) {
                if (!key.equals(common ? CanonicalQuestionKey.of(lines.group(1)) : key(lines.group(1)))
                        && (contact == null || contact != companyContactIntent(lines.group(1)))) continue;
                String answer = lines.group(2).strip();
                if (answer.length() > AssistantOutputValidator.MAX_RESPONSE_TEXT_LENGTH) continue;
                if (found != null && !found.answer().equals(answer)) return new Lookup(null, true);
                found = new Match(answer, source.chunkId());
            }
        }
        return new Lookup(found, false);
    }

    private enum ContactKind { PHONE, EMAIL }

    private static ContactKind companyContactIntent(String question) {
        String text = " " + VietnameseQueryNormalizer.fold(question).replaceAll("[^\\p{L}\\p{N}]+", " ").strip() + " ";
        if (!text.contains(" coparking ")) return null;
        // These questions need their own source/context, not a company contact answer.
        if (text.matches("(?s).*\\b(?:bai|ban do|toa do|tai khoan|dang nhap|xac minh|mat khau|quyen|nhan vien|khach hang|"
                + "nguoi khac|cua toi|hoa don|thanh toan|hoan tien|phi|gia|dat cho|ve thang|"
                + "sua|doi|thay|xoa|cap nhat|mau thuan|nguon|cu|moi|bao lau|phan hoi|uu tien|"
                + "gio|luc|mo cua|tai sao|vi sao|khong|chua|loi)\\b.*")) return null;
        boolean phone = text.matches("(?s).*\\b(?:hotline|so dien thoai|dien thoai lien he|so lien he)\\b.*");
        boolean email = text.matches("(?s).*\\b(?:email|thu dien tu)\\b.*");
        if (phone == email) return null;
        return phone ? ContactKind.PHONE : ContactKind.EMAIL;
    }

    private static String key(String question) {
        // Keep all words, negations, numbers and internal punctuation; only accent and final question marks differ.
        return VietnameseQueryNormalizer.normalize(question).folded().replaceAll("[?.!]+$", "").strip();
    }
}
