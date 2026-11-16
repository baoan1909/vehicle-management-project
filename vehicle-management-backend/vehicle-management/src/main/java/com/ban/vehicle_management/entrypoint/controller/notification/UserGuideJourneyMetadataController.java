package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.userguide.mapper.UserGuideJourneyMetadataApiMapper;
import com.ban.vehicle_management.application.notification.userguide.port.in.UserGuideJourneyMetadataPortIn;
import com.ban.vehicle_management.entrypoint.dto.notification.userguide.response.UserGuideJourneyMetadataResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/user-guides/journey-metadata")
public class UserGuideJourneyMetadataController {

    private final UserGuideJourneyMetadataPortIn metadataPortIn;
    private final UserGuideJourneyMetadataApiMapper mapper;

    public UserGuideJourneyMetadataController(
            UserGuideJourneyMetadataPortIn metadataPortIn,
            UserGuideJourneyMetadataApiMapper mapper
    ) {
        this.metadataPortIn = metadataPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('USER_GUIDE_READ_ALL')")
    public ResponseEntity<ApiResponse<UserGuideJourneyMetadataResponse>> getMetadata() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched user guide journey metadata successfully",
                mapper.toResponse(metadataPortIn.getMetadata())));
    }
}
