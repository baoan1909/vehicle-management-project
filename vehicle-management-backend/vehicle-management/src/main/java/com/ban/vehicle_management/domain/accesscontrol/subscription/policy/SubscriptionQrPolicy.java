package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionQrVerificationStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SubscriptionQrPolicy {

    private final LicensePlatePolicy licensePlatePolicy = new LicensePlatePolicy();

    public boolean issueIfAbsent(Subscription subscription, String token, Instant issuedAt) {
        requireActive(subscription);
        if (subscription.getQrRevokedAt() != null) {
            throw new ConflictException("Subscription QR credential has been revoked");
        }
        if (subscription.getQrAccessToken() != null) {
            return false;
        }
        if (token == null || token.isBlank() || issuedAt == null) {
            throw new IllegalArgumentException("QR token and issuedAt are required");
        }
        subscription.setQrAccessToken(token);
        subscription.setQrIssuedAt(issuedAt);
        return true;
    }

    public void refresh(Subscription subscription, String token, Instant issuedAt) {
        requireActive(subscription);
        requireQrInput(token, issuedAt);
        subscription.setQrAccessToken(token);
        subscription.setQrIssuedAt(issuedAt);
        subscription.setQrRevokedAt(null);
    }

    public boolean blockAccess(Subscription subscription, UUID actorAccountId, Instant blockedAt) {
        requireActive(subscription);
        requireActorAndTime(actorAccountId, blockedAt);
        if (Boolean.TRUE.equals(subscription.getAccessBlocked())) {
            return false;
        }
        subscription.setAccessBlocked(true);
        subscription.setAccessBlockedAt(blockedAt);
        subscription.setAccessBlockedBy(actorAccountId);
        subscription.setAccessRestoredAt(null);
        subscription.setAccessRestoredBy(null);
        return true;
    }

    public boolean unblockAccess(Subscription subscription, UUID actorAccountId, Instant restoredAt) {
        requireActive(subscription);
        requireActorAndTime(actorAccountId, restoredAt);
        if (!Boolean.TRUE.equals(subscription.getAccessBlocked())) {
            return false;
        }
        subscription.setAccessBlocked(false);
        subscription.setAccessRestoredAt(restoredAt);
        subscription.setAccessRestoredBy(actorAccountId);
        return true;
    }

    public SubscriptionQrVerificationStatus verify(
            Subscription subscription,
            LocalDate businessDate,
            UUID requestedParkingLotId,
            String expectedLicensePlate,
            String observedLicensePlate
    ) {
        if (subscription.getQrRevokedAt() != null) {
            return SubscriptionQrVerificationStatus.REVOKED;
        }
        if (subscription.getStatus() == SubscriptionStatus.EXPIRED
                || businessDate.isAfter(subscription.getEffectiveTo())) {
            return SubscriptionQrVerificationStatus.EXPIRED;
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            return SubscriptionQrVerificationStatus.INACTIVE;
        }
        if (businessDate.isBefore(subscription.getEffectiveFrom())) {
            return SubscriptionQrVerificationStatus.NOT_YET_VALID;
        }
        if (Boolean.TRUE.equals(subscription.getAccessBlocked())) {
            return SubscriptionQrVerificationStatus.BLOCKED;
        }
        if (!requestedParkingLotId.equals(subscription.getParkingLotId())) {
            return SubscriptionQrVerificationStatus.WRONG_PARKING_LOT;
        }
        if (observedLicensePlate != null && !observedLicensePlate.isBlank()
                && (expectedLicensePlate == null || expectedLicensePlate.isBlank()
                || !licensePlatePolicy.matches(expectedLicensePlate, observedLicensePlate))) {
            return SubscriptionQrVerificationStatus.VEHICLE_MISMATCH;
        }
        return SubscriptionQrVerificationStatus.VALID;
    }

    private void requireActive(Subscription subscription) {
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("Only an active subscription can manage access credentials");
        }
    }

    private void requireQrInput(String token, Instant issuedAt) {
        if (token == null || token.isBlank() || issuedAt == null) {
            throw new IllegalArgumentException("QR token and issuedAt are required");
        }
    }

    private void requireActorAndTime(UUID actorAccountId, Instant actionAt) {
        if (actorAccountId == null || actionAt == null) {
            throw new IllegalArgumentException("Actor account and action time are required");
        }
    }
}
