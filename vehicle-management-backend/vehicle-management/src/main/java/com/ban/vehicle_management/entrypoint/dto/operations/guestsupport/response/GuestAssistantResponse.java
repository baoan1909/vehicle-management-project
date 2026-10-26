package com.ban.vehicle_management.entrypoint.dto.operations.guestsupport.response;

import java.util.List;

public record GuestAssistantResponse(
        String answer,
        List<Citation> citations,
        boolean handoffRecommended,
        String diagnosticCode
) {
    public record Citation(String label, String title, Integer page, String section) {
    }
}
