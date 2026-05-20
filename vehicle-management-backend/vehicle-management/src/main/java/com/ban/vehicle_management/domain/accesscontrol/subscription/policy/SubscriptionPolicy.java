package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SubscriptionPolicy {

    public void initializeNewSubscription(Subscription subscription) {
        requireSubscription(subscription);

        if (subscription.getStatus() == null) {
            subscription.setStatus(SubscriptionStatus.PENDING);
        }

        validateState(subscription);
    }

    public void activate(Subscription subscription, UUID approvedBy, Instant approvedAt) {
        requireStatus(subscription, SubscriptionStatus.PENDING);
        requireField(approvedBy, "approvedBy");
        requireField(approvedAt, "approvedAt");

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setApprovedBy(approvedBy);
        subscription.setApprovedAt(approvedAt);
        validateState(subscription);
    }

    public void reject(Subscription subscription) {
        requireStatus(subscription, SubscriptionStatus.PENDING);

        subscription.setStatus(SubscriptionStatus.REJECTED);
        subscription.setCardId(null);
        subscription.setCardReceiptDate(null);
        validateState(subscription);
    }

    public void cancel(Subscription subscription) {
        requireSubscription(subscription);
        if (subscription.getStatus() != SubscriptionStatus.PENDING && subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Subscription can only be cancelled from PENDING or ACTIVE status");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        validateState(subscription);
    }

    public void expire(Subscription subscription, LocalDate expiredOn) {
        requireStatus(subscription, SubscriptionStatus.ACTIVE);
        requireField(expiredOn, "expiredOn");

        if (expiredOn.isBefore(subscription.getEffectiveTo())) {
            throw new BadRequestException("Subscription cannot expire before effectiveTo");
        }

        subscription.setStatus(SubscriptionStatus.EXPIRED);
        validateState(subscription);
    }

    public void assignCard(Subscription subscription, UUID cardId, LocalDate cardReceiptDate) {
        requireSubscription(subscription);
        if (subscription.getStatus() == SubscriptionStatus.REJECTED
                || subscription.getStatus() == SubscriptionStatus.CANCELLED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new BadRequestException("Card can only be assigned to active workflow subscription");
        }

        requireField(cardId, "cardId");
        subscription.setCardId(cardId);
        subscription.setCardReceiptDate(cardReceiptDate);
        validateState(subscription);
    }

    public void validateState(Subscription subscription) {
        requireSubscription(subscription);
        requireField(subscription.getCustomerId(), "customerId");
        requireField(subscription.getCustomerVehicleId(), "customerVehicleId");
        requireField(subscription.getTicketTypeId(), "ticketTypeId");
        requireField(subscription.getEffectiveFrom(), "effectiveFrom");
        requireField(subscription.getEffectiveTo(), "effectiveTo");
        requireField(subscription.getPrice(), "price");
        requireField(subscription.getStatus(), "status");

        if (subscription.getEffectiveTo().isBefore(subscription.getEffectiveFrom())) {
            throw new BadRequestException("effectiveTo must not be before effectiveFrom");
        }

        if (subscription.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("price must not be negative");
        }

        if (subscription.getCardReceiptDate() != null && subscription.getCardId() == null) {
            throw new BadRequestException("cardId is required when cardReceiptDate is provided");
        }

        boolean hasApprovalMetadata = subscription.getApprovedBy() != null || subscription.getApprovedAt() != null;
        boolean hasFullApprovalMetadata = subscription.getApprovedBy() != null && subscription.getApprovedAt() != null;

        switch (subscription.getStatus()) {
            case PENDING, REJECTED -> {
                if (hasApprovalMetadata) {
                    throw new BadRequestException("Pending or rejected subscription must not keep approval metadata");
                }
            }
            case ACTIVE -> {
                if (!hasFullApprovalMetadata) {
                    throw new BadRequestException("Active subscription must have approvedBy and approvedAt");
                }
            }
            case EXPIRED -> {
                if (!hasFullApprovalMetadata) {
                    throw new BadRequestException("Expired subscription must keep approval metadata");
                }
            }
            case CANCELLED -> {
                if (subscription.getApprovedBy() == null ^ subscription.getApprovedAt() == null) {
                    throw new BadRequestException("approvedBy and approvedAt must appear together");
                }
            }
        }
    }

    private void requireStatus(Subscription subscription, SubscriptionStatus expectedStatus) {
        requireSubscription(subscription);
        if (subscription.getStatus() != expectedStatus) {
            throw new BadRequestException("Subscription must be in " + expectedStatus + " status");
        }
    }

    private void requireSubscription(Subscription subscription) {
        requireField(subscription, "subscription");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}

