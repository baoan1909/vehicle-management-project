package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.UserGuideApiMapper;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideFeaturePortIn;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideFeature;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.CreateUserGuideFeatureRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UpdateUserGuideFeatureRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UserGuideFeatureFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideFeatureAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/user-guide-features")
public class UserGuideFeatureAdminController {

    private final UserGuideFeaturePortIn featurePortIn;
    private final UserGuideApiMapper apiMapper;

    public UserGuideFeatureAdminController(
            UserGuideFeaturePortIn featurePortIn,
            UserGuideApiMapper apiMapper
    ) {
        this.featurePortIn = featurePortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideFeatureAdminResponse>> createFeature(
            @Valid @RequestBody CreateUserGuideFeatureRequest request
    ) {
        UserGuideFeature created = featurePortIn.createFeature(apiMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "User guide feature created successfully",
                apiMapper.toFeatureAdminResponse(created)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<UserGuideFeatureAdminResponse>>> listFeatures(
            @ModelAttribute UserGuideFeatureFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<UserGuideFeature> page = featurePortIn.listFeatures(filter.enabled(), filter.keyword(), pageable);
        Page<UserGuideFeatureAdminResponse> mapped = page.map(apiMapper::toFeatureAdminResponse);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide features successfully",
                apiMapper.toPageResponse(mapped)));
    }

    @GetMapping("/{featureId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<UserGuideFeatureAdminResponse>> getFeature(
            @PathVariable UUID featureId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide feature successfully",
                apiMapper.toFeatureAdminResponse(featurePortIn.getFeature(featureId))));
    }

    @PutMapping("/{featureId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideFeatureAdminResponse>> updateFeature(
            @PathVariable UUID featureId,
            @Valid @RequestBody UpdateUserGuideFeatureRequest request
    ) {
        UserGuideFeature updated = featurePortIn.updateFeature(featureId, apiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "User guide feature updated successfully",
                apiMapper.toFeatureAdminResponse(updated)));
    }
}
