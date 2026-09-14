package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.mapper.AiToolCallApiMapper;
import com.ban.vehicle_management.application.ai.port.in.AiToolCallPortIn;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.entrypoint.dto.ai.toolcall.response.AiToolCallResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/tool-calls")
public class AiToolCallController {

    private final AiToolCallPortIn aiToolCallPortIn;
    private final AiToolCallApiMapper mapper;

    public AiToolCallController(AiToolCallPortIn aiToolCallPortIn, AiToolCallApiMapper mapper) {
        this.aiToolCallPortIn = aiToolCallPortIn;
        this.mapper = mapper;
    }

    @GetMapping("/{toolCallId}")
    public ResponseEntity<ApiResponse<AiToolCallResponse>> getToolCall(@PathVariable UUID toolCallId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched AI tool call successfully",
                mapper.toResponse(aiToolCallPortIn.getToolCall(toolCallId))
        ));
    }

    @PostMapping("/{toolCallId}/confirm")
    public ResponseEntity<ApiResponse<AiToolCallResponse>> confirm(@PathVariable UUID toolCallId) {
        AiToolCall toolCall = aiToolCallPortIn.confirm(toolCallId);
        return ResponseEntity.ok(ApiResponse.ok("AI tool call confirmed successfully", mapper.toResponse(toolCall)));
    }

    @PostMapping("/{toolCallId}/deny")
    public ResponseEntity<ApiResponse<AiToolCallResponse>> deny(@PathVariable UUID toolCallId) {
        AiToolCall toolCall = aiToolCallPortIn.deny(toolCallId);
        return ResponseEntity.ok(ApiResponse.ok("AI tool call denied successfully", mapper.toResponse(toolCall)));
    }
}
