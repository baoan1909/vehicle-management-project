package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import com.ban.vehicle_management.application.notification.notification.model.BroadcastNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRecipientCriteria;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy;
import com.ban.vehicle_management.domain.operations.approvalrequest.policy.OnboardingApprovalPolicy.OnboardingApprovalKind;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.util.UUID;

public final class ApprovalNotificationSupport {

    private static final OnboardingApprovalPolicy ONBOARDING_APPROVAL_POLICY = new OnboardingApprovalPolicy();

    private ApprovalNotificationSupport() {
    }

    public static void notifyAccount(
            NotificationPortIn notificationPortIn,
            UUID accountId,
            NotificationType notificationType,
            String title,
            String message,
            String redirectUrl,
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
                redirectUrl,
                relatedSchema,
                relatedTable,
                relatedId
        ));
    }

    public static void notifyApprovers(
            NotificationPortIn notificationPortIn,
            ApprovalRequest approvalRequest,
            String targetRoleCode,
            NotificationType notificationType,
            String title
    ) {
        if (notificationPortIn == null) {
            return;
        }
        OnboardingApprovalPolicy.ReviewerAudience audience = ONBOARDING_APPROVAL_POLICY.resolveReviewerAudience(
                approvalRequest.getRequestType(),
                targetRoleCode,
                approvalRequest.getRequestedBy()
        );
        notificationPortIn.sendBroadcastWebNotification(new BroadcastNotificationCommand(
                true,
                null,
                null,
                null,
                notificationType,
                title,
                "Có yêu cầu phê duyệt mới cần xử lý.",
                redirectUrl(audience.kind()),
                approvalRequest.getTargetSchema(),
                approvalRequest.getTargetTable(),
                approvalRequest.getTargetId(),
                new NotificationRecipientCriteria(
                        true,
                        audience.requiredPermissionCodes(),
                        audience.excludedAccountIds(),
                        true
                )
        ));
    }

    private static String redirectUrl(OnboardingApprovalKind kind) {
        return switch (kind) {
            case SYSTEM_ADMIN -> "/admin/account?tab=onboarding&kind=system-admin";
            case INTERNAL_EMPLOYEE -> "/admin/account?tab=onboarding&kind=internal-employee";
            case CUSTOMER -> "/admin/account?tab=onboarding&kind=customer";
        };
    }
}
