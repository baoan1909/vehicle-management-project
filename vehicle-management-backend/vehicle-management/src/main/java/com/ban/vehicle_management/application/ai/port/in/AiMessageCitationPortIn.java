package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.ai.model.AiMessageCitation;
import java.util.List;
import java.util.UUID;

public interface AiMessageCitationPortIn {

    /** Citations of one assistant message, visible only to conversation members. */
    List<AiMessageCitation> citationsForMessage(UUID messageId);
}
