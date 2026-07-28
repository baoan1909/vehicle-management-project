package com.ban.vehicle_management.application.accesscontrol.subscription.port.in;

import java.time.Instant;

public interface SubscriptionPaymentTimeoutPortIn {

    int cancelExpiredPendingPayments(Instant now);
}
