package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.notification.notification.model.SendNotificationCommand;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.notification.NotificationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionExpirationUseCaseImpl implements SubscriptionExpirationPortIn {

    private static final int EXPIRING_SOON_DAYS = 7;

    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerPortOut customerPortOut;
    private final NotificationPortIn notificationPortIn;

    public SubscriptionExpirationUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            CustomerPortOut customerPortOut,
            NotificationPortIn notificationPortIn
    ) {
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerPortOut = customerPortOut;
        this.notificationPortIn = notificationPortIn;
    }

    @Override
    @Transactional
    public int expireExpiredSubscriptions() {
        return expireExpiredSubscriptions(LocalDate.now(DateTimeUtils.VIETNAM_ZONE));
    }

    int expireExpiredSubscriptions(LocalDate businessDate) {
        if (businessDate == null) {
            throw new BadRequestException("businessDate must not be null");
        }

        notifyExpiringSoonSubscriptions(businessDate.plusDays(EXPIRING_SOON_DAYS));
        List<Subscription> expiredSubscriptions = subscriptionPortOut.findActiveSubscriptionsExpiredBefore(businessDate);
        int expiredCount = subscriptionPortOut.expireActiveSubscriptionsBefore(businessDate);
        expiredSubscriptions.forEach(this::notifyExpiredSubscription);
        return expiredCount;
    }

    private void notifyExpiringSoonSubscriptions(LocalDate effectiveTo) {
        subscriptionPortOut.findActiveSubscriptionsExpiringOn(effectiveTo)
                .forEach(this::notifyExpiringSoonSubscription);
    }

    private void notifyExpiringSoonSubscription(Subscription subscription) {
        sendCustomerNotification(
                subscription,
                NotificationType.SUBSCRIPTION_EXPIRING_SOON,
                "Vé đăng ký sắp hết hạn",
                "Vé đăng ký của bạn sẽ hết hạn vào " + subscription.getEffectiveTo() + ". Vui lòng gia hạn nếu cần."
        );
    }

    private void notifyExpiredSubscription(Subscription subscription) {
        sendCustomerNotification(
                subscription,
                NotificationType.SUBSCRIPTION_EXPIRED,
                "Vé đăng ký đã hết hạn",
                "Vé đăng ký của bạn đã hết hạn. Vui lòng gia hạn hoặc đăng ký vé mới nếu cần."
        );
    }

    private void sendCustomerNotification(
            Subscription subscription,
            NotificationType notificationType,
            String title,
            String message
    ) {
        if (notificationPortIn == null) {
            return;
        }
        customerPortOut.findAccountIdByCustomerId(subscription.getCustomerId())
                .ifPresent(accountId -> notificationPortIn.sendWebNotification(new SendNotificationCommand(
                        accountId,
                        notificationType,
                        title,
                        message,
                        "access_control",
                        "subscriptions",
                        subscription.getSubscriptionId()
                )));
    }
}
