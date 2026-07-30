package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationAudience;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import java.util.UUID;

final class ApprovalNotificationSupport {

    private ApprovalNotificationSupport() {
    }

    static void notifyAccount(
            NotificationPortIn notificationPortIn,
            UUID accountId,
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
            String title
    ) {
        if (notificationPortIn == null) {
            return;
        }
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                false,
                NotificationAudience.APPROVERS,
                null,
                title,
                "Co yeu cau phe duyet moi can xu ly.",
                approvalRequest.getTargetSchema(),
                approvalRequest.getTargetTable(),
                approvalRequest.getTargetId()
        ));
    }
}
