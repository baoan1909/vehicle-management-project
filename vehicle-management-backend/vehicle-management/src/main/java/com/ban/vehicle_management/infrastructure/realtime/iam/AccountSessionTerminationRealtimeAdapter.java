package com.ban.vehicle_management.infrastructure.realtime.iam;

import com.ban.vehicle_management.application.iam.account.port.out.AccountSessionTerminationPortOut;
import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationChannel;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationStatus;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountSessionTerminationRealtimeAdapter implements AccountSessionTerminationPortOut {

    private static final String RELATED_SCHEMA = "iam";
    private static final String RELATED_TABLE = "account_deletion_requests";

    private final NotificationRealtimeEventPublisherPortOut realtimePublisher;

    public AccountSessionTerminationRealtimeAdapter(NotificationRealtimeEventPublisherPortOut realtimePublisher) {
        this.realtimePublisher = realtimePublisher;
    }

    @Override
    public void publish(UUID accountId, UUID deletionRequestId, Instant occurredAt) {
        realtimePublisher.publish(new NotificationRealtimeMessage(
                null,
                accountId,
                null,
                NotificationChannel.WEB,
                NotificationType.ACCOUNT_STATUS_CHANGED,
                "Phiên đăng nhập đã kết thúc",
                "Tài khoản đang trong thời gian chờ xóa.",
                NotificationStatus.SENT,
                occurredAt,
                null,
                "/",
                RELATED_SCHEMA,
                RELATED_TABLE,
                deletionRequestId,
                occurredAt
        ));
    }
}
