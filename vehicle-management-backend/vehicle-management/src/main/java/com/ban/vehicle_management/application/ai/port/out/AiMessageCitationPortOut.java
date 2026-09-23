package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import java.util.List;
import java.util.UUID;

public interface AiMessageCitationPortOut {

    List<AiMessageCitation> saveAll(UUID messageId, List<AiMessageCitation> citations);

    List<AiMessageCitation> findByMessageId(UUID messageId);
}
