package com.ban.vehicle_management.application.ai.query;

import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MessageCitationsResult(
        UUID messageId,
        List<AiMessageCitation> citations,
        BigDecimal groundedConfidence,
        boolean handoffRecommended,
        String diagnosticCode
) {
}
