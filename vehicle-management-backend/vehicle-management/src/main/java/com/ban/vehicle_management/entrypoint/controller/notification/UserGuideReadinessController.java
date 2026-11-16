package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.PostOnboardingReadinessApiMapper;
import com.ban.vehicle_management.application.notification.userguide.model.result.PostOnboardingReadinessResult;
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
@RequestMapping("/api/notifications/user-guides")
public class UserGuideReadinessController {
    private final PostOnboardingReadinessPortIn readinessPortIn;
    private final PostOnboardingReadinessApiMapper readinessApiMapper;

    public UserGuideReadinessController(
            PostOnboardingReadinessPortIn readinessPortIn,
            PostOnboardingReadinessApiMapper readinessApiMapper
    ) {
        this.readinessPortIn = readinessPortIn;
        this.readinessApiMapper = readinessApiMapper;
    }

    @GetMapping("/post-onboarding-readiness")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostOnboardingReadinessResponse>> getPostOnboardingReadiness(
            @RequestParam(required = false) UUID parkingLotId
    ) {
        PostOnboardingReadinessResult result = readinessPortIn.getReadiness(parkingLotId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched post-onboarding readiness successfully",
                readinessApiMapper.toResponse(result)
        ));
    }
}
