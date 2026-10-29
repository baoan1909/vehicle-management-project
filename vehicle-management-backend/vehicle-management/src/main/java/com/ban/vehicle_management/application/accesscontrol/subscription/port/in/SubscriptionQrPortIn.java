package com.ban.vehicle_management.application.accesscontrol.subscription.port.in;

import com.ban.vehicle_management.application.accesscontrol.subscription.model.command.VerifySubscriptionQrCommand;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrCredential;
import com.ban.vehicle_management.application.accesscontrol.subscription.model.result.SubscriptionQrVerification;
import java.util.UUID;

public interface SubscriptionQrPortIn {

    SubscriptionQrCredential issueMyQrCredential(UUID subscriptionId);

    SubscriptionQrVerification verifyQrCredential(VerifySubscriptionQrCommand command);

    SubscriptionQrVerification verifyQrImage(byte[] imageBytes, UUID parkingLotId, String licensePlate);
}
