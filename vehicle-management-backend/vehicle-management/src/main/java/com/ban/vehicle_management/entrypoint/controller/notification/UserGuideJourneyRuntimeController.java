package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.PostOnboardingReadinessApiMapper;
import com.ban.vehicle_management.application.notification.userguide.port.in.PostOnboardingReadinessPortIn;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.PostOnboardingReadinessResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user-guides/journeys")
public class UserGuideJourneyRuntimeController {
    private final PostOnboardingReadinessPortIn readinessPortIn;
    private final PostOnboardingReadinessApiMapper mapper;

    public UserGuideJourneyRuntimeController(
            PostOnboardingReadinessPortIn readinessPortIn,
            PostOnboardingReadinessApiMapper mapper
    ) {
        this.readinessPortIn = readinessPortIn;
        this.mapper = mapper;
    }

    @GetMapping("/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostOnboardingReadinessResponse>> resolve(
            @RequestParam(defaultValue = "OPERATIONS_ONBOARDING") String featureCode,
            @RequestParam(defaultValue = "/ADMIN/HEADER") String contextKey,
            @RequestParam(required = false) UUID parkingLotId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Resolved user guide journey successfully",
                mapper.toResponse(readinessPortIn.resolveJourney(featureCode, contextKey, parkingLotId))));
    }
}
