package com.ban.vehicle_management.domain.notification.userguide.policy;

import java.util.Set;

public final class UserGuideJourneyReadinessKeys {
    public static final Set<String> SUPPORTED = Set.of(
            "ACCOUNT_ACCESS", "PARKING_SCOPE", "CATALOG_PRICING", "PARKING_TOPOLOGY",
            "PARKING_ACTIVATION", "WORKFORCE_SHIFT", "CHECK_IN", "FIND_SESSION",
            "PREPARE_CHECK_OUT", "COLLECT_PAYMENT", "COMPLETE_CHECK_OUT", "EXCEPTIONS");

    private UserGuideJourneyReadinessKeys() {
    }
}
