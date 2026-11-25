package com.ban.vehicle_management.entrypoint.controller.ai;

import com.ban.vehicle_management.application.ai.port.in.AiRuntimeModePortIn;
import com.ban.vehicle_management.application.ai.mapper.AiRuntimeApiMapper;
import com.ban.vehicle_management.entrypoint.dto.ai.model.request.ChangeAiRuntimeModeRequest;
import com.ban.vehicle_management.entrypoint.dto.ai.model.response.AiRuntimeModeResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/runtime")
public class AiRuntimeModeController {
    private final AiRuntimeModePortIn runtime;
    private final AiRuntimeApiMapper mapper;
    public AiRuntimeModeController(AiRuntimeModePortIn runtime, AiRuntimeApiMapper mapper) {
        this.runtime = runtime;
        this.mapper = mapper;
    }
    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_MODEL_READ_ALL')")
    public ApiResponse<AiRuntimeModeResponse> state() {
        return ApiResponse.ok("Đã lấy chế độ AI", mapper.toResponse(runtime.state()));
    }
    @PutMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('AI_MODEL_MANAGE_ALL')")
    public ApiResponse<AiRuntimeModeResponse> change(@Valid @RequestBody ChangeAiRuntimeModeRequest request) {
        return ApiResponse.ok("Đã tiếp nhận yêu cầu chuyển chế độ", mapper.toResponse(runtime.changeMode(request.mode(), request.expectedEpoch())));
    }
}
