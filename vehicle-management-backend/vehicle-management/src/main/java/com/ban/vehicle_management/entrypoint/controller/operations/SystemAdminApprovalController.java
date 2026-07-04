package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.SystemAdminApprovalApiMapper;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SystemAdminApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.SystemAdminApprovalPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.SystemAdminApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.SystemAdminApprovalAdminResponse;
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
@RequestMapping("/api/operations/approval-requests/system-admin-onboarding")
public class SystemAdminApprovalController {

    private final SystemAdminApprovalPortIn systemAdminApprovalPortIn;
    private final SystemAdminApprovalApiMapper systemAdminApprovalApiMapper;

    public SystemAdminApprovalController(
            SystemAdminApprovalPortIn systemAdminApprovalPortIn,
            SystemAdminApprovalApiMapper systemAdminApprovalApiMapper
    ) {
        this.systemAdminApprovalPortIn = systemAdminApprovalPortIn;
        this.systemAdminApprovalApiMapper = systemAdminApprovalApiMapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<List<SystemAdminApprovalAdminResponse>>> getSystemAdminApprovals(
            @ModelAttribute SystemAdminApprovalFilterRequest request
    ) {
        List<SystemAdminApprovalResult> results = systemAdminApprovalPortIn.getSystemAdminApprovals(
                systemAdminApprovalApiMapper.toFilterCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched system admin approval requests successfully",
                systemAdminApprovalApiMapper.toResponses(results)
        ));
    }

    @GetMapping("/{approvalRequestId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_READ_ALL')")
    public ResponseEntity<ApiResponse<SystemAdminApprovalAdminResponse>> getSystemAdminApprovalById(
            @PathVariable UUID approvalRequestId
    ) {
        SystemAdminApprovalResult result = systemAdminApprovalPortIn.getSystemAdminApprovalById(approvalRequestId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched system admin approval request successfully",
                systemAdminApprovalApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SystemAdminApprovalAdminResponse>> getMyLatestSystemAdminApproval() {
        SystemAdminApprovalResult result = systemAdminApprovalPortIn.getMyLatestSystemAdminApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched system admin approval status successfully",
                systemAdminApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<SystemAdminApprovalAdminResponse>> approveSystemAdminApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        SystemAdminApprovalResult result = systemAdminApprovalPortIn.approveSystemAdminApproval(
                approvalRequestId,
                systemAdminApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "System admin approval request approved successfully",
                systemAdminApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ACCOUNT_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<SystemAdminApprovalAdminResponse>> rejectSystemAdminApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        SystemAdminApprovalResult result = systemAdminApprovalPortIn.rejectSystemAdminApproval(
                approvalRequestId,
                systemAdminApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "System admin approval request rejected successfully",
                systemAdminApprovalApiMapper.toResponse(result)
        ));
    }

    @PostMapping("/me/resubmit")
    public ResponseEntity<ApiResponse<SystemAdminApprovalAdminResponse>> resubmitMySystemAdminApproval() {
        SystemAdminApprovalResult result = systemAdminApprovalPortIn.resubmitMySystemAdminApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "System admin approval request resubmitted successfully",
                systemAdminApprovalApiMapper.toResponse(result)
        ));
    }
}
