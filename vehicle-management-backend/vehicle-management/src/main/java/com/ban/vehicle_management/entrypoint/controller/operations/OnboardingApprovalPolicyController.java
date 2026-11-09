package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.OnboardingApprovalPolicyApiMapper;
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
            "@permissionAuthorizer.hasPermission('ORGANIZATION_REGISTRATION_REVIEW_ALL') "
                    + "or @permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')";

    private final OnboardingApprovalPolicyPortIn policyPortIn;
    private final OnboardingApprovalPolicyApiMapper mapper;

    public OnboardingApprovalPolicyController(
            OnboardingApprovalPolicyPortIn policyPortIn,
            OnboardingApprovalPolicyApiMapper mapper
    ) {
        this.policyPortIn = policyPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize(REQUIRED_PERMISSIONS)
    public ResponseEntity<ApiResponse<OnboardingApprovalPoliciesResponse>> getPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched onboarding approval policies successfully",
                mapper.toResponse(policyPortIn.getPolicies())
        ));
    }

    @PutMapping
    @PreAuthorize(REQUIRED_PERMISSIONS)
    public ResponseEntity<ApiResponse<OnboardingApprovalPoliciesResponse>> updatePolicies(
            @RequestBody UpdateOnboardingApprovalPoliciesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Updated onboarding approval policies successfully",
                mapper.toResponse(policyPortIn.updatePolicies(mapper.toCommand(request)))
        ));
    }
}
