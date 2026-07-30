package com.ban.vehicle_management.entrypoint.controller.notification;

import com.ban.vehicle_management.application.notification.notification.mapper.NotificationApiMapper;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.notification.notification.model.Notification;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.request.NotificationFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUnreadCountResponse;
import com.ban.vehicle_management.entrypoint.dto.notification.notification.response.NotificationUserResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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

    public NotificationController(
            NotificationPortIn notificationPortIn,
            NotificationApiMapper notificationApiMapper
    ) {
        this.notificationPortIn = notificationPortIn;
        this.notificationApiMapper = notificationApiMapper;
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
