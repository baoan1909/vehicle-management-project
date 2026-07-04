package com.ban.vehicle_management.entrypoint.controller.operations;

import com.ban.vehicle_management.application.operations.approvalrequest.mapper.InternalEmployeeApprovalApiMapper;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.InternalEmployeeApprovalResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.in.InternalEmployeeApprovalPortIn;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.InternalEmployeeApprovalFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.request.ReviewInternalEmployeeApprovalRequest;
import com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response.InternalEmployeeApprovalAdminResponse;
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
@RequestMapping("/api/operations/approval-requests/internal-employee-onboarding")
public class InternalEmployeeApprovalController {

    private final InternalEmployeeApprovalPortIn internalEmployeeApprovalPortIn;
    private final InternalEmployeeApprovalApiMapper internalEmployeeApprovalApiMapper;

    public InternalEmployeeApprovalController(
            InternalEmployeeApprovalPortIn internalEmployeeApprovalPortIn,
            InternalEmployeeApprovalApiMapper internalEmployeeApprovalApiMapper
    ) {
        this.internalEmployeeApprovalPortIn = internalEmployeeApprovalPortIn;
        this.internalEmployeeApprovalApiMapper = internalEmployeeApprovalApiMapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('ACCOUNT_READ_ALL', 'EMPLOYEE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<InternalEmployeeApprovalAdminResponse>>> getInternalEmployeeApprovals(
            @ModelAttribute InternalEmployeeApprovalFilterRequest request
    ) {
        List<InternalEmployeeApprovalResult> results = internalEmployeeApprovalPortIn.getInternalEmployeeApprovals(
                internalEmployeeApprovalApiMapper.toFilterCommand(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched internal employee approval requests successfully",
                internalEmployeeApprovalApiMapper.toResponses(results)
        ));
    }

    @GetMapping("/{approvalRequestId}")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('ACCOUNT_READ_ALL', 'EMPLOYEE_READ_ALL')")
    public ResponseEntity<ApiResponse<InternalEmployeeApprovalAdminResponse>> getInternalEmployeeApprovalById(
            @PathVariable UUID approvalRequestId
    ) {
        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortIn.getInternalEmployeeApprovalById(approvalRequestId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched internal employee approval request successfully",
                internalEmployeeApprovalApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<InternalEmployeeApprovalAdminResponse>> getMyLatestInternalEmployeeApproval() {
        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortIn.getMyLatestInternalEmployeeApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched internal employee approval status successfully",
                internalEmployeeApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('ACCOUNT_UPDATE_ALL', 'EMPLOYEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<InternalEmployeeApprovalAdminResponse>> approveInternalEmployeeApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortIn.approveInternalEmployeeApproval(
                approvalRequestId,
                internalEmployeeApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Internal employee approval request approved successfully",
                internalEmployeeApprovalApiMapper.toResponse(result)
        ));
    }

    @PatchMapping("/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('ACCOUNT_UPDATE_ALL', 'EMPLOYEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<InternalEmployeeApprovalAdminResponse>> rejectInternalEmployeeApproval(
            @PathVariable UUID approvalRequestId,
            @RequestBody(required = false) ReviewInternalEmployeeApprovalRequest request
    ) {
        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortIn.rejectInternalEmployeeApproval(
                approvalRequestId,
                internalEmployeeApprovalApiMapper.toReviewCommand(
                        request == null ? new ReviewInternalEmployeeApprovalRequest(null) : request
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Internal employee approval request rejected successfully",
                internalEmployeeApprovalApiMapper.toResponse(result)
        ));
    }

    @PostMapping("/me/resubmit")
    public ResponseEntity<ApiResponse<InternalEmployeeApprovalAdminResponse>> resubmitMyInternalEmployeeApproval() {
        InternalEmployeeApprovalResult result = internalEmployeeApprovalPortIn.resubmitMyInternalEmployeeApproval();
        return ResponseEntity.ok(ApiResponse.ok(
                "Internal employee approval request resubmitted successfully",
                internalEmployeeApprovalApiMapper.toResponse(result)
        ));
    }
}
