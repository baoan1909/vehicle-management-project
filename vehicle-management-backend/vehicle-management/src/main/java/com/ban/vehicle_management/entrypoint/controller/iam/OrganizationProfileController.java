package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.organizationprofile.mapper.OrganizationProfileApiMapper;
import com.ban.vehicle_management.application.iam.organizationprofile.port.in.OrganizationProfilePortIn;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.RequestOrganizationProfileUpdateRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.ReviewOrganizationProfileUpdateRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationProfileResponse;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationProfileUpdateRequestResponse;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/iam")
public class OrganizationProfileController {
    private final OrganizationProfilePortIn organizationProfilePortIn;
    private final OrganizationProfileApiMapper organizationProfileApiMapper;

    public OrganizationProfileController(OrganizationProfilePortIn organizationProfilePortIn, OrganizationProfileApiMapper organizationProfileApiMapper) {
        this.organizationProfilePortIn = organizationProfilePortIn;
        this.organizationProfileApiMapper = organizationProfileApiMapper;
    }

    @GetMapping("/organizations/my-profile")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_PROFILE_READ_OWN')")
    public ResponseEntity<ApiResponse<OrganizationProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched organization profile successfully",
                organizationProfileApiMapper.toResponse(organizationProfilePortIn.getMyProfile())));
    }

    @PostMapping("/organizations/my-profile/update-requests")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_PROFILE_UPDATE_REQUEST_OWN')")
    public ResponseEntity<ApiResponse<OrganizationProfileUpdateRequestResponse>> requestProfileUpdate(@RequestBody RequestOrganizationProfileUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Organization profile update request submitted successfully",
                organizationProfileApiMapper.toResponse(organizationProfilePortIn.requestMyProfileUpdate(organizationProfileApiMapper.toCommand(request)))));
    }

    @GetMapping("/organization-profile-update-requests")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_PROFILE_REVIEW_ALL')")
    public ResponseEntity<ApiResponse<List<OrganizationProfileUpdateRequestResponse>>> getProfileUpdateRequests(@RequestParam(required = false) ApprovalRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched organization profile update requests successfully",
                organizationProfilePortIn.getProfileUpdateRequests(status).stream().map(organizationProfileApiMapper::toResponse).toList()));
    }

    @PostMapping("/organization-profile-update-requests/{approvalRequestId}/approve")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_PROFILE_REVIEW_ALL')")
    public ResponseEntity<ApiResponse<OrganizationProfileUpdateRequestResponse>> approveProfileUpdate(@PathVariable UUID approvalRequestId,
                                                                                                         @RequestBody(required = false) ReviewOrganizationProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Organization profile update request approved successfully",
                organizationProfileApiMapper.toResponse(organizationProfilePortIn.approveProfileUpdate(approvalRequestId, organizationProfileApiMapper.toCommand(request)))));
    }

    @PostMapping("/organization-profile-update-requests/{approvalRequestId}/reject")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_PROFILE_REVIEW_ALL')")
    public ResponseEntity<ApiResponse<OrganizationProfileUpdateRequestResponse>> rejectProfileUpdate(@PathVariable UUID approvalRequestId,
                                                                                                        @RequestBody(required = false) ReviewOrganizationProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Organization profile update request rejected successfully",
                organizationProfileApiMapper.toResponse(organizationProfilePortIn.rejectProfileUpdate(approvalRequestId, organizationProfileApiMapper.toCommand(request)))));
    }
}
