package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.port.in.AssistantStatusPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.entrypoint.dto.ai.assistant.response.AssistantMessageStatusResponse;
import com.ban.vehicle_management.entrypoint.dto.ai.assistant.response.AssistantStatusResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/assistant")
public class AssistantController {

    private final AssistantStatusPortIn assistantStatusPortIn;
    private final CurrentAccountPortIn currentAccountPortIn;

    public AssistantController(
            AssistantStatusPortIn assistantStatusPortIn,
            CurrentAccountPortIn currentAccountPortIn
    ) {
        this.assistantStatusPortIn = assistantStatusPortIn;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @GetMapping("/status")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SUPPORT_WIDGET_ACCESS_OWN')")
    public ResponseEntity<ApiResponse<AssistantStatusResponse>> getStatus() {
        currentAccountPortIn.requirePermission("SUPPORT_WIDGET_ACCESS_OWN");
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched assistant status successfully",
                new AssistantStatusResponse(assistantStatusPortIn.isAssistantEnabled())
        ));
    }

    @GetMapping("/messages/{inputMessageId}/status")
    @PreAuthorize("@permissionAuthorizer.hasPermission('SUPPORT_WIDGET_ACCESS_OWN')")
    public ResponseEntity<ApiResponse<AssistantMessageStatusResponse>> getMessageStatus(@PathVariable UUID inputMessageId) {
        currentAccountPortIn.requirePermission("SUPPORT_WIDGET_ACCESS_OWN");
        AssistantStatusPortIn.MessageStatus status = assistantStatusPortIn.getMessageStatus(inputMessageId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched assistant message status successfully",
                new AssistantMessageStatusResponse(
                        status.inputMessageId(),
                        status.status(),
                        status.errorCode(),
                        status.terminal()
                )
        ));
    }
}
