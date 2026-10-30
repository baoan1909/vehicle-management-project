package com.ban.vehicle_management.application.accesscontrol.subscription.port.out;

import java.util.Optional;

public interface SubscriptionQrImagePortOut {

    String encodePngDataUrl(String payload);

    Optional<String> decode(byte[] imageBytes);
}
