package com.ban.vehicle_management.ai.golden;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Golden-set evaluation mechanics for Phase 5 retrieval. A question is
 * "recalled" at K when any of the top-K returned titles contains at least one
 * expected keyword (case- and accent-insensitive containment on both sides).
 */
public final class GoldenEvaluationRunner {

    private GoldenEvaluationRunner() {
    }

    public record GoldenQuestion(
            String id, String question, String questionNoAccent,
            List<String> expectedKeywords, String expectedScope, int minCitations) {
    }

    public record RetrievalRun(
            List<String> titles, List<String> scopes, List<String> citationLabels) {
    }

    public record EvaluationReport(
            int total,
            double recallAt1,
            double recallAt3,
            double recallAt5,
            double mrr,
            double accentEquivalence,
            double citationCorrectness,
            double scopeLeakage,
            Map<String, Double> recallPerQuestion
    ) {
    }

    public static EvaluationReport evaluate(
            List<GoldenQuestion> questions, Function<GoldenQuestion, RetrievalRun> retriever) {
        int total = questions.size();
        int hit1 = 0;
        int hit3 = 0;
        int hit5 = 0;
        double reciprocalSum = 0.0;
        int accentPairs = 0;
        int accentAgreements = 0;
        int citationsChecked = 0;
        int citationsValid = 0;
        int scopeLeaks = 0;
        Map<String, Double> perQuestion = new HashMap<>();
        Map<String, Integer> firstRankByBase = new HashMap<>();

        for (GoldenQuestion question : questions) {
            RetrievalRun run = retriever.apply(question);
            List<String> titles = run.titles() == null ? List.of() : run.titles();
            int firstRank = firstMatchingRank(titles, question.expectedKeywords());
            perQuestion.put(question.id(), firstRank < 0 ? 0.0 : 1.0 / firstRank);
            if (firstRank == 1) {
                hit1++;
            }
            if (firstRank > 0 && firstRank <= 3) {
                hit3++;
            }
            if (firstRank > 0 && firstRank <= 5) {
                hit5++;
                reciprocalSum += 1.0 / firstRank;
            }
            if (question.questionNoAccent() != null && !question.questionNoAccent().isBlank()) {
                String base = question.questionNoAccent();
                if (firstRankByBase.containsKey(base)) {
                    accentPairs++;
                    if (firstRankByBase.get(base).equals(firstRank)) {
                        accentAgreements++;
                    }
                } else {
                    firstRankByBase.put(base, firstRank);
                }
            }
            if (run.citationLabels() != null) {
                for (String label : run.citationLabels()) {
                    citationsChecked++;
                    if (label != null && label.matches("C\\d+") && !label.equals("C0")) {
                        citationsValid++;
                    }
                }
            }
            if (run.scopes() != null) {
                for (String scope : run.scopes()) {
                    if (!question.expectedScope().equals(scope)
                            && !"PUBLIC".equals(scope)) {
                        scopeLeaks++;
                    }
                }
            }
        }
        return new EvaluationReport(
                total,
                total == 0 ? 0 : (double) hit1 / total,
                total == 0 ? 0 : (double) hit3 / total,
                total == 0 ? 0 : (double) hit5 / total,
                total == 0 ? 0 : reciprocalSum / total,
                accentPairs == 0 ? 1.0 : (double) accentAgreements / accentPairs,
                citationsChecked == 0 ? 1.0 : (double) citationsValid / citationsChecked,
                total == 0 ? 0 : (double) scopeLeaks / total,
                Map.copyOf(perQuestion));
    }

    private static int firstMatchingRank(List<String> titles, List<String> expectedKeywords) {
        for (int index = 0; index < Math.min(titles.size(), 5); index++) {
            String title = titles.get(index) == null ? "" : titles.get(index).toLowerCase(Locale.ROOT);
            for (String keyword : expectedKeywords) {
                String folded = keyword.toLowerCase(Locale.ROOT);
                if (!title.isEmpty() && (title.contains(folded) || folded.contains(title))) {
                    return index + 1;
                }
            }
        }
        return -1;
    }
}
