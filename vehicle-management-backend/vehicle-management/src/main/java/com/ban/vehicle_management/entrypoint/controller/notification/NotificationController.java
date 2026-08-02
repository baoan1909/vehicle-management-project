package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.iam.role.port.in.RolePortIn;
import com.ban.vehicle_management.application.notification.notification.mapper.NotificationApiMapper;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.iam.role.model.Role;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.request.NotificationFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationActiveRoleResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUnreadCountResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUserResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationPortIn notificationPortIn;
    private final NotificationApiMapper notificationApiMapper;
    private final RolePortIn rolePortIn;

    public NotificationController(
            NotificationPortIn notificationPortIn,
            NotificationApiMapper notificationApiMapper,
            RolePortIn rolePortIn
    ) {
        this.notificationPortIn = notificationPortIn;
        this.notificationApiMapper = notificationApiMapper;
        this.rolePortIn = rolePortIn;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationUserResponse>>> getMyNotifications(
            @ModelAttribute NotificationFilterRequest request
    ) {
        List<Notification> notifications = notificationPortIn.getMyNotifications(
                Boolean.TRUE.equals(request.unreadOnly()),
                request.limit() == null ? 0 : request.limit(),
                request.beforeCreatedAt()
        );
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched notifications successfully",
                notificationApiMapper.toUserResponses(notifications)
        ));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> countMyUnreadNotifications() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched unread notification count successfully",
                new NotificationUnreadCountResponse(notificationPortIn.countMyUnread())
        ));
    }

    @GetMapping("/active-roles")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission(" +
            "'BROADCAST_NOTIFICATION_READ_ALL', " +
            "'BROADCAST_NOTIFICATION_CREATE_ALL', " +
            "'BROADCAST_NOTIFICATION_UPDATE_ALL', " +
            "'BROADCAST_NOTIFICATION_PUBLISH_ALL')")
    public ResponseEntity<ApiResponse<List<NotificationActiveRoleResponse>>> getActiveRolesForAnnouncements() {
        List<NotificationActiveRoleResponse> response = rolePortIn.getRoles(true, null, null).stream()
                .sorted(Comparator.comparing(Role::getCode))
                .map(role -> new NotificationActiveRoleResponse(
                        role.getRoleId(),
                        role.getCode(),
                        role.getName()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched active notification roles successfully",
                response
        ));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationUserResponse>> markMyNotificationAsRead(
            @PathVariable UUID notificationId
    ) {
        Notification notification = notificationPortIn.markMyNotificationAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Notification marked as read successfully",
                notificationApiMapper.toUserResponse(notification)
        ));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllMyNotificationsAsRead() {
        notificationPortIn.markAllMyNotificationsAsRead();
        return ResponseEntity.ok(ApiResponse.ok("Notifications marked as read successfully"));
    }
}
