package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.CustomerOnboardingApprovalApiMapper;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.CustomerOnboardingApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.CustomerOnboardingApprovalPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.CustomerOnboardingApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.CustomerOnboardingApprovalAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations/approval-requests/customer-onboarding")
public class CustomerOnboardingApprovalController {

    private final CustomerOnboardingApprovalPortIn customerOnboardingApprovalPortIn;
    private final CustomerOnboardingApprovalApiMapper customerOnboardingApprovalApiMapper;

    public CustomerOnboardingApprovalController(
            CustomerOnboardingApprovalPortIn customerOnboardingApprovalPortIn,
            CustomerOnboardingApprovalApiMapper customerOnboardingApprovalApiMapper
    ) {
        this.customerOnboardingApprovalPortIn = customerOnboardingApprovalPortIn;
        this.customerOnboardingApprovalApiMapper = customerOnboardingApprovalApiMapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')")
    public ResponseEntity<ApiResponse<List<CustomerOnboardingApprovalAdminResponse>>> getCustomerOnboardingApprovals(
            @ModelAttribute CustomerOnboardingApprovalFilterRequest request
    ) {
        List<CustomerOnboardingApprovalResult> results = customerOnboardingApprovalPortIn
                .getCustomerOnboardingApprovals(customerOnboardingApprovalApiMapper.toFilterCommand(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer onboarding approval requests successfully",
                customerOnboardingApprovalApiMapper.toResponses(results)
        ));
    }

    @GetMapping("/{approvalRequestId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')")
    public ResponseEntity<ApiResponse<CustomerOnboardingApprovalAdminResponse>> getCustomerOnboardingApprovalById(
            @PathVariable UUID approvalRequestId
    ) {
        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortIn
                .getCustomerOnboardingApprovalById(approvalRequestId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer onboarding approval request successfully",
                customerOnboardingApprovalApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerOnboardingApprovalAdminResponse>> getMyLatestCustomerOnboardingApproval() {
        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortIn
                .getMyLatestCustomerOnboardingApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer onboarding approval status successfully",
                customerOnboardingApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')")
    public ResponseEntity<ApiResponse<CustomerOnboardingApprovalAdminResponse>> approveCustomerOnboardingApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortIn.approveCustomerOnboardingApproval(
                approvalRequestId,
                customerOnboardingApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer onboarding approval request approved successfully",
                customerOnboardingApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ONBOARDING_APPROVAL_REVIEW_CUSTOMER_ALL')")
    public ResponseEntity<ApiResponse<CustomerOnboardingApprovalAdminResponse>> rejectCustomerOnboardingApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortIn.rejectCustomerOnboardingApproval(
                approvalRequestId,
                customerOnboardingApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer onboarding approval request rejected successfully",
                customerOnboardingApprovalApiMapper.toResponse(result)
        ));
    }

    @PostMapping("/me/resubmit")
    public ResponseEntity<ApiResponse<CustomerOnboardingApprovalAdminResponse>> resubmitMyCustomerOnboardingApproval() {
        CustomerOnboardingApprovalResult result = customerOnboardingApprovalPortIn
                .resubmitMyCustomerOnboardingApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "Customer onboarding approval request resubmitted successfully",
                customerOnboardingApprovalApiMapper.toResponse(result)
        ));
    }
}
