package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionQrPolicyTest {

    private static final UUID LOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 10, 29);
    private final SubscriptionQrPolicy policy = new SubscriptionQrPolicy();

    @Test
    void shouldIssueQrOnceForActiveSubscription() {
        Subscription subscription = activeSubscription();
        Instant issuedAt = Instant.parse("2026-10-29T03:00:00Z");

        assertTrue(policy.issueIfAbsent(subscription, "token", issuedAt));
        assertEquals("token", subscription.getQrAccessToken());
        assertEquals(issuedAt, subscription.getQrIssuedAt());
        assertFalse(policy.issueIfAbsent(subscription, "another-token", issuedAt.plusSeconds(1)));
        assertEquals("token", subscription.getQrAccessToken());
    }

    @Test
    void shouldIssueQrForPhysicalCardSubscription() {
        Subscription subscription = activeSubscription();
        subscription.setPhysicalCardRequested(true);
        Instant issuedAt = Instant.parse("2026-10-29T03:00:00Z");

        assertTrue(policy.issueIfAbsent(subscription, "token", issuedAt));
        assertEquals("token", subscription.getQrAccessToken());
        assertEquals(issuedAt, subscription.getQrIssuedAt());
    }

    @Test
    void shouldVerifyScopeVehicleAndLifecycle() {
        Subscription subscription = activeSubscription();

        assertEquals(SubscriptionQrVerificationStatus.VALID,
                policy.verify(subscription, BUSINESS_DATE, LOT_ID, "59A1-123.45", "59a112345"));
        assertEquals(SubscriptionQrVerificationStatus.WRONG_PARKING_LOT,
                policy.verify(subscription, BUSINESS_DATE, UUID.randomUUID(), "59A1-123.45", null));
        assertEquals(SubscriptionQrVerificationStatus.VEHICLE_MISMATCH,
                policy.verify(subscription, BUSINESS_DATE, LOT_ID, "59A1-123.45", "51A-999.99"));

        subscription.setEffectiveFrom(BUSINESS_DATE.plusDays(1));
        assertEquals(SubscriptionQrVerificationStatus.NOT_YET_VALID,
                policy.verify(subscription, BUSINESS_DATE, LOT_ID, "59A1-123.45", null));

        subscription.setEffectiveFrom(BUSINESS_DATE.minusDays(30));
        subscription.setEffectiveTo(BUSINESS_DATE.minusDays(1));
        assertEquals(SubscriptionQrVerificationStatus.EXPIRED,
                policy.verify(subscription, BUSINESS_DATE, LOT_ID, "59A1-123.45", null));

        subscription.setQrRevokedAt(Instant.parse("2026-10-29T02:00:00Z"));
        assertEquals(SubscriptionQrVerificationStatus.REVOKED,
                policy.verify(subscription, BUSINESS_DATE, LOT_ID, "59A1-123.45", null));
    }

    private Subscription activeSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(UUID.randomUUID());
        subscription.setParkingLotId(LOT_ID);
        subscription.setCustomerVehicleId(UUID.randomUUID());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPhysicalCardRequested(false);
        subscription.setEffectiveFrom(BUSINESS_DATE.minusDays(1));
        subscription.setEffectiveTo(BUSINESS_DATE.plusDays(30));
        return subscription;
    }
}
