package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionExpirationPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionExpirationUseCaseImpl implements SubscriptionExpirationPortIn {

    private final SubscriptionPortOut subscriptionPortOut;

    public SubscriptionExpirationUseCaseImpl(SubscriptionPortOut subscriptionPortOut) {
        this.subscriptionPortOut = subscriptionPortOut;
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

        return subscriptionPortOut.expireActiveSubscriptionsBefore(businessDate);
    }
}
