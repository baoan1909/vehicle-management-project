package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.UpdateOnboardingApprovalPoliciesCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.OnboardingApprovalPoliciesResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.OnboardingApprovalPolicyPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.UpdateOnboardingApprovalPoliciesRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.OnboardingApprovalPoliciesResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations/onboarding-approval-policies")
public class OnboardingApprovalPolicyController {

    private static final String REQUIRED_PERMISSIONS =
            "@permissionAuthorizer.hasPermission('ORGANIZATION_CREATE_ALL') "
                    + "or @permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')";

    private final OnboardingApprovalPolicyPortIn policyPortIn;

    public OnboardingApprovalPolicyController(OnboardingApprovalPolicyPortIn policyPortIn) {
        this.policyPortIn = policyPortIn;
    }

    @GetMapping
    @PreAuthorize(REQUIRED_PERMISSIONS)
    public ResponseEntity<ApiResponse<OnboardingApprovalPoliciesResponse>> getPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched onboarding approval policies successfully",
                toResponse(policyPortIn.getPolicies())
        ));
    }

    @PutMapping
    @PreAuthorize(REQUIRED_PERMISSIONS)
    public ResponseEntity<ApiResponse<OnboardingApprovalPoliciesResponse>> updatePolicies(
            @RequestBody UpdateOnboardingApprovalPoliciesRequest request
    ) {
        OnboardingApprovalPoliciesResult result = policyPortIn.updatePolicies(
                new UpdateOnboardingApprovalPoliciesCommand(
                        request.customerAutoApproveEnabled(),
                        request.partnerAutoApproveEnabled(),
                        request.avatarAutoApproveEnabled()
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Updated onboarding approval policies successfully",
                toResponse(result)
        ));
    }

    private OnboardingApprovalPoliciesResponse toResponse(OnboardingApprovalPoliciesResult result) {
        return new OnboardingApprovalPoliciesResponse(
                result.customerAutoApproveEnabled(),
                result.customerEffectiveFrom(),
                result.partnerAutoApproveEnabled(),
                result.partnerEffectiveFrom(),
                result.avatarAutoApproveEnabled(),
                result.avatarEffectiveFrom()
        );
    }
}
