package com.ban.vehicle_management.infrastructure.realtime.notification;

import com.ban.vehicle_management.application.notification.notification.model.NotificationRealtimeMessage;
import com.ban.vehicle_management.application.notification.notification.port.out.NotificationPortOut;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationRealtimeRabbitConsumer {

    public static final String USER_QUEUE_DESTINATION = "/user/queue/notifications";
    public static final String USER_TOPIC_DESTINATION = "/user/topic/notifications";
    private static final String QUEUE_SEND_DESTINATION = "/queue/notifications";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRealtimeRabbitConsumer.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry simpUserRegistry;
    private final NotificationPortOut notificationPortOut;

    public NotificationRealtimeRabbitConsumer(
            SimpMessagingTemplate messagingTemplate,
            SimpUserRegistry simpUserRegistry,
            NotificationPortOut notificationPortOut
    ) {
        this.messagingTemplate = messagingTemplate;
        this.simpUserRegistry = simpUserRegistry;
        this.notificationPortOut = notificationPortOut;
    }

    @RabbitListener(queues = "#{notificationRealtimeQueue.name}")
    @Transactional
    public void handle(NotificationRealtimeMessage message) {
        if (message == null || message.accountId() == null) {
            LOGGER.warn("Skip invalid notification realtime message {}", message);
            return;
        }

        String user = message.accountId().toString();
        messagingTemplate.convertAndSendToUser(user, QUEUE_SEND_DESTINATION, message);
        if (message.notificationId() != null && hasNotificationSubscription(user)) {
            notificationPortOut.markRealtimeDelivered(message.notificationId(), Instant.now());
        }
    }

    public static boolean isNotificationDestination(String destination) {
        return USER_QUEUE_DESTINATION.equals(destination) || USER_TOPIC_DESTINATION.equals(destination);
    }

    private boolean hasNotificationSubscription(String userName) {
        SimpUser user = simpUserRegistry.getUser(userName);
        if (user == null) {
            return false;
        }
        for (SimpSession session : user.getSessions()) {
            for (SimpSubscription subscription : session.getSubscriptions()) {
                if (isNotificationDestination(subscription.getDestination())) {
                    return true;
                }
            }
        }
        return false;
    }
}
