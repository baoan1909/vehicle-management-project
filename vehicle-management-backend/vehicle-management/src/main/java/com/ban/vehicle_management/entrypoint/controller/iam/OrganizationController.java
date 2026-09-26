package com.ban.vehicle_management.entrypoint.controller.iam;

import com.ban.vehicle_management.application.iam.organization.mapper.OrganizationApiMapper;
import com.ban.vehicle_management.application.iam.organization.port.in.OrganizationPortIn;
import com.ban.vehicle_management.domain.iam.organization.model.Organization;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.AssignParkingManagerRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.request.CreateOrganizationRequest;
import com.ban.vehicle_management.entrypoint.dto.iam.organization.response.OrganizationAdminResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam/organizations")
public class OrganizationController {

    private final OrganizationPortIn organizationPortIn;
    private final OrganizationApiMapper organizationApiMapper;

    public OrganizationController(
            OrganizationPortIn organizationPortIn,
            OrganizationApiMapper organizationApiMapper
    ) {
        this.organizationPortIn = organizationPortIn;
        this.organizationApiMapper = organizationApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_CREATE_ALL')")
    public ResponseEntity<ApiResponse<OrganizationAdminResponse>> createOrganization(
            @RequestBody CreateOrganizationRequest request
    ) {
        Organization organization = organizationPortIn.createOrganization(
                organizationApiMapper.toDomain(request),
                request.partnerAdminAccountId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Organization created successfully",
                organizationApiMapper.toAdminResponse(organization)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_READ_ALL')")
    public ResponseEntity<ApiResponse<List<OrganizationAdminResponse>>> getOrganizations() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched organizations successfully",
                organizationApiMapper.toAdminResponses(organizationPortIn.getAccessibleOrganizations())
        ));
    }

    @GetMapping("/{organizationId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_READ_ALL')")
    public ResponseEntity<ApiResponse<OrganizationAdminResponse>> getOrganizationById(
            @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched organization successfully",
                organizationApiMapper.toAdminResponse(organizationPortIn.getOrganizationById(organizationId))
        ));
    }

    @PostMapping("/{organizationId}/parking-managers/scopes")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> assignParkingManager(
            @PathVariable UUID organizationId,
            @RequestBody AssignParkingManagerRequest request
    ) {
        organizationPortIn.assignParkingManager(
                organizationId,
                request.parkingManagerAccountId(),
                request.parkingLotIds()
        );
        return ResponseEntity.ok(ApiResponse.ok("Parking manager scope updated successfully"));
    }

    @PostMapping("/{organizationId}/parking-lots/{parkingLotId}/parking-managers/{parkingManagerAccountId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('ORGANIZATION_MEMBERSHIP_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> assignParkingManagerToParkingLot(
            @PathVariable UUID organizationId,
            @PathVariable UUID parkingLotId,
            @PathVariable UUID parkingManagerAccountId
    ) {
        organizationPortIn.assignParkingManagerToParkingLot(organizationId, parkingManagerAccountId, parkingLotId);
        return ResponseEntity.ok(ApiResponse.ok("Parking manager assigned to parking lot successfully"));
    }
}
