package com.ban.vehicle_management.application.accesscontrol.subscription.port.in;

import com.ban.vehicle_management.application.accesscontrol.subscription.model.command.VerifySubscriptionQrCommand;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrCredential;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrVerification;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import java.util.UUID;

public interface SubscriptionQrPortIn {

    SubscriptionQrCredential issueMyQrCredential(UUID subscriptionId);

    SubscriptionQrCredential refreshMyQrCredential(UUID subscriptionId);

    SubscriptionQrCredential blockMySubscriptionAccess(UUID subscriptionId);

    SubscriptionQrCredential unblockMySubscriptionAccess(UUID subscriptionId);

    Subscription blockSubscriptionAccess(UUID subscriptionId);

    Subscription unblockSubscriptionAccess(UUID subscriptionId);

    SubscriptionQrVerification verifyQrCredential(VerifySubscriptionQrCommand command);

    SubscriptionQrVerification verifyQrImage(byte[] imageBytes, UUID parkingLotId, String licensePlate);
}
