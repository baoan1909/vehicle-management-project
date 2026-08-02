package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.broadcastannouncement.mapper.BroadcastAnnouncementApiMapper;
import com.ban.vehicle_management.application.notification.broadcastannouncement.port.in.BroadcastAnnouncementPortIn;
import com.ban.vehicle_management.domain.notification.broadcastannouncement.model.BroadcastAnnouncement;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.BroadcastAnnouncementFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.CreateBroadcastAnnouncementRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.UpdateBroadcastAnnouncementRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.request.UpdateBroadcastAnnouncementDisplayOrderRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.broadcastannouncement.response.BroadcastAnnouncementAdminResponse;
import com.ban.vehicle_management.entrypoint.message.ControllerMessages;
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
@RequestMapping("/api/notifications/broadcast-announcements")
public class BroadcastAnnouncementController {

    private final BroadcastAnnouncementPortIn broadcastAnnouncementPortIn;
    private final BroadcastAnnouncementApiMapper broadcastAnnouncementApiMapper;

    public BroadcastAnnouncementController(
            BroadcastAnnouncementPortIn broadcastAnnouncementPortIn,
            BroadcastAnnouncementApiMapper broadcastAnnouncementApiMapper
    ) {
        this.broadcastAnnouncementPortIn = broadcastAnnouncementPortIn;
        this.broadcastAnnouncementApiMapper = broadcastAnnouncementApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_CREATE_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> create(
            @RequestBody CreateBroadcastAnnouncementRequest request
    ) {
        BroadcastAnnouncement created = broadcastAnnouncementPortIn.createBroadcastAnnouncement(
                broadcastAnnouncementApiMapper.toDomain(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                ControllerMessages.CREATE_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(created)
        ));
    }

    @GetMapping("/{broadcastId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_READ_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> getById(
            @PathVariable UUID broadcastId
    ) {
        BroadcastAnnouncement announcement = broadcastAnnouncementPortIn.getBroadcastAnnouncementById(broadcastId);
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.FETCH_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(announcement)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_READ_ALL')")
    public ResponseEntity<ApiResponse<List<BroadcastAnnouncementAdminResponse>>> getAll(
            @ModelAttribute BroadcastAnnouncementFilterRequest request
    ) {
        List<BroadcastAnnouncement> announcements = broadcastAnnouncementPortIn.getBroadcastAnnouncements(
                request.status()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.FETCH_LIST_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponses(announcements)
        ));
    }

    @PutMapping("/{broadcastId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> update(
            @PathVariable UUID broadcastId,
            @RequestBody UpdateBroadcastAnnouncementRequest request
    ) {
        BroadcastAnnouncement updated = broadcastAnnouncementPortIn.updateBroadcastAnnouncement(
                broadcastId,
                broadcastAnnouncementApiMapper.toDomain(request)
        );
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.UPDATE_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping("/{broadcastId}/display-order")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> updateDisplayOrder(
            @PathVariable UUID broadcastId,
            @RequestBody UpdateBroadcastAnnouncementDisplayOrderRequest request
    ) {
        BroadcastAnnouncement updated = broadcastAnnouncementPortIn.updateBroadcastAnnouncementDisplayOrder(
                broadcastId,
                request.displayOrder()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.UPDATE_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(updated)
        ));
    }

    @PatchMapping("/{broadcastId}/publish")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_PUBLISH_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> publish(
            @PathVariable UUID broadcastId
    ) {
        BroadcastAnnouncement published = broadcastAnnouncementPortIn.publishBroadcastAnnouncement(broadcastId);
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.ACTION_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(published)
        ));
    }

    @PatchMapping("/{broadcastId}/cancel")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_CANCEL_ALL')")
    public ResponseEntity<ApiResponse<BroadcastAnnouncementAdminResponse>> cancel(
            @PathVariable UUID broadcastId
    ) {
        BroadcastAnnouncement cancelled = broadcastAnnouncementPortIn.cancelBroadcastAnnouncement(broadcastId);
        return ResponseEntity.ok(ApiResponse.ok(
                ControllerMessages.ACTION_SUCCESS,
                broadcastAnnouncementApiMapper.toAdminResponse(cancelled)
        ));
    }

    @DeleteMapping("/{broadcastId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('BROADCAST_NOTIFICATION_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID broadcastId
    ) {
        broadcastAnnouncementPortIn.deleteBroadcastAnnouncement(broadcastId);
        return ResponseEntity.ok(ApiResponse.ok(ControllerMessages.DELETE_SUCCESS));
    }
}
