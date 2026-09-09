package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.OnboardingApprovalSummaryApiMapper;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalSummaryResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.OnboardingApprovalSummaryPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.OnboardingApprovalSummaryResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations/approval-requests/onboarding-summary")
public class OnboardingApprovalSummaryController {

    private final OnboardingApprovalSummaryPortIn onboardingApprovalSummaryPortIn;
    private final OnboardingApprovalSummaryApiMapper onboardingApprovalSummaryApiMapper;

    public OnboardingApprovalSummaryController(
            OnboardingApprovalSummaryPortIn onboardingApprovalSummaryPortIn,
            OnboardingApprovalSummaryApiMapper onboardingApprovalSummaryApiMapper
    ) {
        this.onboardingApprovalSummaryPortIn = onboardingApprovalSummaryPortIn;
        this.onboardingApprovalSummaryApiMapper = onboardingApprovalSummaryApiMapper;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OnboardingApprovalSummaryResponse>> getMyPendingSummary() {
        OnboardingApprovalSummaryResult result = onboardingApprovalSummaryPortIn.getMyPendingSummary();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched onboarding approval pending summary successfully",
                onboardingApprovalSummaryApiMapper.toResponse(result)
        ));
    }
}
