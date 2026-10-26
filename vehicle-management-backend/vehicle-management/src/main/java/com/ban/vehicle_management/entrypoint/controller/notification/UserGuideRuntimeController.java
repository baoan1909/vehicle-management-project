package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.UserGuideApiMapper;
import com.ban.vehicle_management.application.notification.userguide.model.result.ResolvedUserGuide;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideRuntimePortIn;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.RecordUserGuideEventRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideUserResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideUserStateResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-guides")
public class UserGuideRuntimeController {

    private final UserGuideRuntimePortIn runtimePortIn;
    private final UserGuideApiMapper apiMapper;

    public UserGuideRuntimeController(UserGuideRuntimePortIn runtimePortIn, UserGuideApiMapper apiMapper) {
        this.runtimePortIn = runtimePortIn;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/resolve")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<List<UserGuideUserResponse>>> resolveGuides(
            @RequestParam(required = false) String featureCode,
            @RequestParam(required = false) String contextKey
    ) {
        List<UserGuideUserResponse> guides = runtimePortIn.resolveGuides(featureCode, contextKey).stream()
                .map(this::toUserResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Resolved user guides successfully", guides));
    }

    @GetMapping("/{guideCode}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<UserGuideUserResponse>> getPublishedGuide(
            @PathVariable String guideCode
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide successfully",
                toUserResponse(runtimePortIn.getPublishedGuide(guideCode))));
    }

    @PostMapping("/{guideId}/events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserGuideUserStateResponse>> recordEvent(
            @PathVariable UUID guideId,
            @Valid @RequestBody RecordUserGuideEventRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "User guide event recorded successfully",
                apiMapper.toUserStateResponse(runtimePortIn.recordEvent(
                        guideId, request.event(), request.versionNumber(), request.stepId()))));
    }

    private UserGuideUserResponse toUserResponse(ResolvedUserGuide resolved) {
        return apiMapper.toUserResponse(
                resolved.guide(), resolved.version(), resolved.userState(), resolved.mediaUrls());
    }
}
