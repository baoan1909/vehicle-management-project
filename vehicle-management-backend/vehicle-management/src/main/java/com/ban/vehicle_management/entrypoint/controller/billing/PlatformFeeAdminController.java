package com.ban.vehicle_management.entrypoint.controller.billing;

import com.ban.vehicle_management.application.billing.platformfee.mapper.PlatformFeeApiMapper;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeTypeDetailResult;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeManagementPortIn;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.CreatePlatformFeeTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.CreatePlatformFeeVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.PlatformFeeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.PreviewPlatformFeesRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.UpdatePlatformFeeTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.request.UpdatePlatformFeeVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeePageResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeePreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeTypeAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeTypeDetailResponse;
import com.ban.vehicle_management.entrypoint.dto.billing.platformfee.response.PlatformFeeVersionAdminResponse;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing/admin/platform-fees")
public class PlatformFeeAdminController {

    private final PlatformFeeManagementPortIn managementPortIn;
    private final PlatformFeeApiMapper apiMapper;

    public PlatformFeeAdminController(
            PlatformFeeManagementPortIn managementPortIn,
            PlatformFeeApiMapper apiMapper) {
        this.managementPortIn = managementPortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeAdminResponse>> createFeeType(
            @RequestBody CreatePlatformFeeTypeRequest request) {
        PlatformFeeType created = managementPortIn.createFeeType(apiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Platform fee type created successfully",
                apiMapper.toAdminResponse(created)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_READ_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeePageResponse>> searchFeeTypes(
            @ModelAttribute PlatformFeeFilterRequest request) {
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        PlatformFeePageResult result = managementPortIn.searchFeeTypes(
                request.keyword(), request.status(), request.transactionType(), request.feeBasis(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched platform fee types successfully",
                apiMapper.toPageResponse(result)));
    }

    @PostMapping("/preview")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_PREVIEW_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeePreviewResponse>> previewFees(
            @RequestBody PreviewPlatformFeesRequest request) {
        if (request.grossAmount() == null || request.customerPaidAmount() == null) {
            throw new BadRequestException("grossAmount and customerPaidAmount must not be null");
        }
        PlatformFeePreviewResult result = managementPortIn.previewFees(
                request.transactionType(), request.effectiveAt(),
                request.grossAmount(), request.customerPaidAmount());
        return ResponseEntity.ok(ApiResponse.ok(
                "Previewed platform fees successfully",
                apiMapper.toPreviewResponse(result)));
    }

    @GetMapping("/{feeTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_READ_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeDetailResponse>> getFeeTypeById(
            @PathVariable UUID feeTypeId) {
        PlatformFeeTypeDetailResult result = managementPortIn.getFeeTypeById(feeTypeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched platform fee type successfully",
                apiMapper.toDetailResponse(result)));
    }

    @PutMapping("/{feeTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeAdminResponse>> updateDraftFeeType(
            @PathVariable UUID feeTypeId,
            @RequestBody UpdatePlatformFeeTypeRequest request) {
        PlatformFeeType updated =
                managementPortIn.updateDraftFeeType(feeTypeId, apiMapper.toDraftChanges(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee type updated successfully",
                apiMapper.toAdminResponse(updated)));
    }

    @PatchMapping("/{feeTypeId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ACTIVATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeAdminResponse>> activateFeeType(
            @PathVariable UUID feeTypeId) {
        PlatformFeeType activated = managementPortIn.activateFeeType(feeTypeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee type activated successfully",
                apiMapper.toAdminResponse(activated)));
    }

    @PatchMapping("/{feeTypeId}/deactivate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ACTIVATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeAdminResponse>> deactivateFeeType(
            @PathVariable UUID feeTypeId) {
        PlatformFeeType deactivated = managementPortIn.deactivateFeeType(feeTypeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee type deactivated successfully",
                apiMapper.toAdminResponse(deactivated)));
    }

    @DeleteMapping("/{feeTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ARCHIVE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeTypeAdminResponse>> archiveFeeType(
            @PathVariable UUID feeTypeId) {
        PlatformFeeType archived = managementPortIn.archiveFeeType(feeTypeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee type archived successfully",
                apiMapper.toAdminResponse(archived)));
    }

    @PostMapping("/{feeTypeId}/versions")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeVersionAdminResponse>> createRuleVersion(
            @PathVariable UUID feeTypeId,
            @RequestBody CreatePlatformFeeVersionRequest request) {
        PlatformFeeRuleVersion created = managementPortIn.createRuleVersion(
                feeTypeId, request.feeRate(), request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Platform fee rule version created successfully",
                apiMapper.toVersionResponse(created)));
    }

    @GetMapping("/{feeTypeId}/versions")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<PlatformFeeVersionAdminResponse>>> getVersionHistory(
            @PathVariable UUID feeTypeId) {
        List<PlatformFeeRuleVersion> versions = managementPortIn.getVersionHistory(feeTypeId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched platform fee versions successfully",
                apiMapper.toVersionResponses(versions)));
    }

    @PutMapping("/{feeTypeId}/versions/{versionId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeVersionAdminResponse>> updateDraftRuleVersion(
            @PathVariable UUID feeTypeId,
            @PathVariable UUID versionId,
            @RequestBody UpdatePlatformFeeVersionRequest request) {
        PlatformFeeRuleVersion updated = managementPortIn.updateDraftRuleVersion(
                feeTypeId, versionId, request.feeRate(), request.effectiveFrom(), request.effectiveTo());
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee rule version updated successfully",
                apiMapper.toVersionResponse(updated)));
    }

    @PatchMapping("/{feeTypeId}/versions/{versionId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ACTIVATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeVersionAdminResponse>> activateRuleVersion(
            @PathVariable UUID feeTypeId,
            @PathVariable UUID versionId) {
        PlatformFeeRuleVersion activated = managementPortIn.activateRuleVersion(feeTypeId, versionId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee rule version activated successfully",
                apiMapper.toVersionResponse(activated)));
    }

    @PatchMapping("/{feeTypeId}/versions/{versionId}/deactivate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ACTIVATE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeVersionAdminResponse>> deactivateRuleVersion(
            @PathVariable UUID feeTypeId,
            @PathVariable UUID versionId) {
        PlatformFeeRuleVersion deactivated = managementPortIn.deactivateRuleVersion(feeTypeId, versionId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee rule version deactivated successfully",
                apiMapper.toVersionResponse(deactivated)));
    }

    @DeleteMapping("/{feeTypeId}/versions/{versionId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PLATFORM_FEE_ARCHIVE_ALL')")
    public ResponseEntity<ApiResponse<PlatformFeeVersionAdminResponse>> archiveRuleVersion(
            @PathVariable UUID feeTypeId,
            @PathVariable UUID versionId) {
        PlatformFeeRuleVersion archived = managementPortIn.archiveRuleVersion(feeTypeId, versionId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Platform fee rule version archived successfully",
                apiMapper.toVersionResponse(archived)));
    }
}
