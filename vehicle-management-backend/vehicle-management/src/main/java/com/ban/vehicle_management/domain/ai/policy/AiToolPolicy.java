package com.ban.vehicle_management.domain.ai.policy;

import java.util.Set;

public class AiToolPolicy {

    private static final Set<String> ALLOWED_TOOLS = Set.of(
            "searchKnowledge",
            "getMyTickets",
            "getMyTicketDetail",
            "prepareSupportTicket",
            "confirmCreateSupportTicket",
            "openTicketConversation",
            "prepareReopenRequest",
            "requestManagerReview"
    );

    public boolean isAllowed(String toolName) {
        return toolName != null && ALLOWED_TOOLS.contains(toolName);
    }

    public Set<String> allowedTools() {
        return ALLOWED_TOOLS;
    }
}
