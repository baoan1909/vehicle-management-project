package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;

final class ApprovalNotificationSupport {

    private ApprovalNotificationSupport() {
    }

    static void notifyAccount(
            NotificationPortIn notificationPortIn,
            UUID accountId,
            NotificationType notificationType,
            String title,
            String message,
            String relatedSchema,
            String relatedTable,
            UUID relatedId
    ) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendWebNotification(new SendNotificationCommand(
                accountId,
                notificationType,
                title,
                message,
                relatedSchema,
                relatedTable,
                relatedId
        ));
    }

    static void notifyApprovers(
            NotificationPortIn notificationPortIn,
            ApprovalRequest approvalRequest,
            NotificationType notificationType,
            String title
    ) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.APPROVERS,
                null,
                null,
                notificationType,
                title,
                "Có yêu cầu phê duyệt mới cần xử lý.",
                null,
                approvalRequest.getTargetSchema(),
                approvalRequest.getTargetTable(),
                approvalRequest.getTargetId()
        ));
    }
}
