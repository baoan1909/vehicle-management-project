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
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new ConflictException("Only an active subscription can receive a QR credential");
        }
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
}
