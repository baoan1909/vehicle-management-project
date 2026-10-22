package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.UserGuideApiMapper;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideAdminPortIn;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuide;
import com.ban.vehicle_management.domain.notification.userguide.model.UserGuideVersion;
import com.ban.vehicle_management.entrypoint.dto.common.PageResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.CreateUserGuideRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.CreateUserGuideVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UpdateUserGuideRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UpdateUserGuideVersionRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.request.UserGuideFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideVersionAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/user-guides")
public class UserGuideAdminController {

    private final UserGuideAdminPortIn adminPortIn;
    private final UserGuideApiMapper apiMapper;

    public UserGuideAdminController(UserGuideAdminPortIn adminPortIn, UserGuideApiMapper apiMapper) {
        this.adminPortIn = adminPortIn;
        this.apiMapper = apiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideAdminResponse>> createGuide(
            @Valid @RequestBody CreateUserGuideRequest request
    ) {
        UserGuide guide = apiMapper.toDomain(request);
        UserGuideVersion initialVersion = toDomainVersion(request);
        UserGuide created = adminPortIn.createGuide(guide, initialVersion, request.featureCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "User guide created successfully",
                apiMapper.toAdminResponse(created)));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<UserGuideAdminResponse>>> listGuides(
            @ModelAttribute UserGuideFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserGuide> page = adminPortIn.listGuides(
                filter.featureId(), filter.status(), filter.displayType(), filter.keyword(), pageable);
        Page<UserGuideAdminResponse> mapped = page.map(apiMapper::toAdminResponse);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guides successfully",
                apiMapper.toPageResponse(mapped)));
    }

    @GetMapping("/{guideId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<UserGuideAdminResponse>> getGuide(@PathVariable UUID guideId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide successfully",
                apiMapper.toAdminResponse(adminPortIn.getGuide(guideId))));
    }

    @PutMapping("/{guideId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideAdminResponse>> updateGuide(
            @PathVariable UUID guideId,
            @Valid @RequestBody UpdateUserGuideRequest request
    ) {
        UserGuide updated = adminPortIn.updateGuide(guideId, apiMapper.toDomain(request), request.lockVersion());
        return ResponseEntity.ok(ApiResponse.ok(
                "User guide updated successfully",
                apiMapper.toAdminResponse(updated)));
    }

    @PostMapping("/{guideId}/versions")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideVersionAdminResponse>> createDraftVersion(
            @PathVariable UUID guideId,
            @Valid @RequestBody CreateUserGuideVersionRequest request
    ) {
        UserGuideVersion draft = toDomainVersion(request);
        UserGuideVersion created =
                adminPortIn.createDraftVersion(guideId, draft, request.cloneFromVersionNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Draft version created successfully",
                apiMapper.toVersionAdminResponse(created)));
    }

    @GetMapping("/{guideId}/versions")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<UserGuideVersionAdminResponse>>> listVersions(
            @PathVariable UUID guideId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide versions successfully",
                apiMapper.toVersionAdminResponses(adminPortIn.listVersions(guideId))));
    }

    @GetMapping("/{guideId}/versions/{versionNumber}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<UserGuideVersionAdminResponse>> getVersion(
            @PathVariable UUID guideId,
            @PathVariable Integer versionNumber
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide version successfully",
                apiMapper.toVersionAdminResponse(adminPortIn.getVersion(guideId, versionNumber))));
    }

    @PutMapping("/{guideId}/versions/{versionNumber}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideVersionAdminResponse>> updateDraftVersion(
            @PathVariable UUID guideId,
            @PathVariable Integer versionNumber,
            @Valid @RequestBody UpdateUserGuideVersionRequest request
    ) {
        UserGuideVersion version = toDomainVersion(request);
        UserGuideVersion updated =
                adminPortIn.updateDraftVersion(guideId, versionNumber, version, request.lockVersion());
        return ResponseEntity.ok(ApiResponse.ok(
                "Draft version updated successfully",
                apiMapper.toVersionAdminResponse(updated)));
    }

    @PostMapping("/{guideId}/versions/{versionNumber}/publish")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_PUBLISH_ALL')")
    public ResponseEntity<ApiResponse<UserGuideVersionAdminResponse>> publishVersion(
            @PathVariable UUID guideId,
            @PathVariable Integer versionNumber
    ) {
        UserGuideVersion published = adminPortIn.publishVersion(guideId, versionNumber);
        return ResponseEntity.ok(ApiResponse.ok(
                "User guide version published successfully",
                apiMapper.toVersionAdminResponse(published)));
    }

    @PostMapping("/{guideId}/archive")
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_ARCHIVE_ALL')")
    public ResponseEntity<ApiResponse<UserGuideAdminResponse>> archiveGuide(@PathVariable UUID guideId) {
        UserGuide archived = adminPortIn.archiveGuide(guideId);
        return ResponseEntity.ok(ApiResponse.ok(
                "User guide archived successfully",
                apiMapper.toAdminResponse(archived)));
    }

    private UserGuideVersion toDomainVersion(CreateUserGuideRequest request) {
        if (request.initialVersion() == null) {
            return null;
        }
        UserGuideVersion version = new UserGuideVersion();
        version.setTitle(request.initialVersion().title());
        version.setSummary(request.initialVersion().summary());
        version.setDeepLink(request.initialVersion().deepLink());
        version.setAudienceMode(request.initialVersion().audienceMode());
        version.setSteps(apiMapper.toDomainSteps(request.initialVersion().steps()));
        version.setAudiences(apiMapper.toDomainAudiences(request.initialVersion().audiences()));
        return version;
    }

    private UserGuideVersion toDomainVersion(CreateUserGuideVersionRequest request) {
        UserGuideVersion version = new UserGuideVersion();
        version.setTitle(request.title());
        version.setSummary(request.summary());
        version.setDeepLink(request.deepLink());
        version.setAudienceMode(request.audienceMode());
        version.setSteps(apiMapper.toDomainSteps(request.steps()));
        version.setAudiences(apiMapper.toDomainAudiences(request.audiences()));
        return version;
    }

    private UserGuideVersion toDomainVersion(UpdateUserGuideVersionRequest request) {
        UserGuideVersion version = new UserGuideVersion();
        version.setTitle(request.title());
        version.setSummary(request.summary());
        version.setDeepLink(request.deepLink());
        version.setAudienceMode(request.audienceMode());
        version.setSteps(request.steps() == null ? null : apiMapper.toDomainSteps(request.steps()));
        version.setAudiences(request.audiences() == null ? null : apiMapper.toDomainAudiences(request.audiences()));
        return version;
    }
}
