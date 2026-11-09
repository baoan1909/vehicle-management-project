package com.ban.vehicle_management.entrypoint.controller.people;

import com.ban.vehicle_management.application.people.userprofile.model.AvatarModerationResult;
import com.ban.vehicle_management.application.people.userprofile.port.in.AvatarModerationPortIn;
import com.ban.vehicle_management.application.people.userprofile.mapper.AvatarModerationApiMapper;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.request.ReviewAvatarRequest;
import com.ban.vehicle_management.entrypoint.dto.people.userprofile.response.AvatarModerationResponse;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/avatar-approvals")
public class AvatarModerationController {
    private final AvatarModerationPortIn portIn;
    private final AvatarModerationApiMapper mapper;

    public AvatarModerationController(AvatarModerationPortIn portIn, AvatarModerationApiMapper mapper) {
        this.portIn = portIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_PROFILE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<AvatarModerationResponse>>> list(
            @RequestParam(required = false) ApprovalRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched avatar approvals successfully",
                mapper.toResponses(portIn.getAvatarApprovals(status))));
    }

    @PostMapping("/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_PROFILE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<AvatarModerationResponse>> approve(
            @PathVariable UUID approvalRequestId, @RequestBody(required = false) ReviewAvatarRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar approved successfully",
                mapper.toResponse(portIn.approve(approvalRequestId, request == null ? null : request.note()))));
    }

    @PostMapping("/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_PROFILE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<AvatarModerationResponse>> reject(
            @PathVariable UUID approvalRequestId, @RequestBody ReviewAvatarRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Avatar rejected successfully",
                mapper.toResponse(portIn.reject(approvalRequestId, request == null ? null : request.note()))));
    }
}
