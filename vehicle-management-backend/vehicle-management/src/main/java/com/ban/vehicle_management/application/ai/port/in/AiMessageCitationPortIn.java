package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.application.ai.query.MessageCitationsResult;
import java.util.UUID;

public interface AiMessageCitationPortIn {

    /** Citations of one assistant message, visible only to conversation members. */
    MessageCitationsResult citationsForMessage(UUID messageId);
}
