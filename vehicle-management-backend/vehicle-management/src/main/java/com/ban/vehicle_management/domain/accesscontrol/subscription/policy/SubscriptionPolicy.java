package com.ban.vehicle_management.domain.accesscontrol.subscription.policy;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class SubscriptionPolicy {

    private static final int MIN_EFFECTIVE_LEAD_DAYS = 2;
    private static final int MAX_EFFECTIVE_LEAD_DAYS = 7;

    public void initializeNewSubscription(Subscription subscription, int durationDays, LocalDate currentDate) {
        requireSubscription(subscription);
        validateDuration(durationDays);
        validateRequestedDate(subscription.getRequestedEffectiveFrom(), currentDate);

        subscription.setEffectiveFrom(subscription.getRequestedEffectiveFrom());
        subscription.setEffectiveTo(calculateEffectiveTo(subscription.getEffectiveFrom(), durationDays));
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setCardId(null);
        subscription.setCardReceiptDate(null);
        clearApproval(subscription);
        clearRejection(subscription);

        validateState(subscription);
    }

    public void preparePendingUpdate(Subscription subscription, int durationDays, LocalDate currentDate) {
        requireStatus(subscription, SubscriptionStatus.PENDING);
        validateDuration(durationDays);
        validateRequestedDate(subscription.getRequestedEffectiveFrom(), currentDate);

        subscription.setEffectiveFrom(subscription.getRequestedEffectiveFrom());
        subscription.setEffectiveTo(calculateEffectiveTo(subscription.getEffectiveFrom(), durationDays));

        validateState(subscription);
    }

    public void approve(Subscription subscription, UUID approvedBy, Instant approvedAt, LocalDate approvalDate,
                        UUID reservedCardId, BigDecimal invoiceFinalAmount) {
        requireStatus(subscription, SubscriptionStatus.PENDING);
        requireField(approvedBy, "approvedBy");
        requireField(approvedAt, "approvedAt");
        requireField(approvalDate, "approvalDate");
        requireField(reservedCardId, "reservedCardId");
        requireField(invoiceFinalAmount, "invoiceFinalAmount");

        if (!approvalDate.isBefore(subscription.getRequestedEffectiveFrom())) {
            throw new ConflictException("Subscription approval deadline has expired");
        }

        if (invoiceFinalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("invoiceFinalAmount must not be negative");
        }

        subscription.setApprovedBy(approvedBy);
        subscription.setApprovedAt(approvedAt);
        subscription.setCardId(reservedCardId);
        subscription.setCardReceiptDate(null);
        clearRejection(subscription);

        if (invoiceFinalAmount.compareTo(BigDecimal.ZERO) == 0) {
            subscription.setStatus(SubscriptionStatus.PENDING_CARD);
        } else {
            subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
        }

        validateState(subscription);
    }

    public void reject(Subscription subscription, String reason, UUID rejectedBy, Instant rejectedAt) {
        requireStatus(subscription, SubscriptionStatus.PENDING);
        requireField(rejectedBy, "rejectedBy");
        requireField(rejectedAt, "rejectedAt");

        subscription.setStatus(SubscriptionStatus.REJECTED);
        subscription.setRejectionReason(TextValidationUtils.normalizeRequiredText(reason, "reason", 500));
        subscription.setRejectedBy(rejectedBy);
        subscription.setRejectedAt(rejectedAt);
        subscription.setCardId(null);
        subscription.setCardReceiptDate(null);
        clearApproval(subscription);

        validateState(subscription);
    }

    public void markPaymentCompleted(Subscription subscription) {
        requireStatus(subscription, SubscriptionStatus.PENDING_PAYMENT);
        subscription.setStatus(SubscriptionStatus.PENDING_CARD);
        validateState(subscription);
    }

    public void assignReservedCard(Subscription subscription, LocalDate cardReceiptDate, int durationDays) {
        requireStatus(subscription, SubscriptionStatus.PENDING_CARD);
        requireField(subscription.getCardId(), "cardId");
        requireField(cardReceiptDate, "cardReceiptDate");
        validateDuration(durationDays);

        LocalDate actualEffectiveFrom = cardReceiptDate.isAfter(subscription.getRequestedEffectiveFrom())
                ? cardReceiptDate
                : subscription.getRequestedEffectiveFrom();

        subscription.setCardReceiptDate(cardReceiptDate);
        subscription.setEffectiveFrom(actualEffectiveFrom);
        subscription.setEffectiveTo(calculateEffectiveTo(actualEffectiveFrom, durationDays));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        validateState(subscription);
    }

    public void cancelBeforeRefundWorkflow(Subscription subscription) {
        requireSubscription(subscription);

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            return;
        }

        if (subscription.getStatus() != SubscriptionStatus.PENDING
                && subscription.getStatus() != SubscriptionStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only PENDING or PENDING_PAYMENT subscription can be cancelled");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        validateState(subscription);
    }

    public void expire(Subscription subscription, LocalDate currentDate) {
        requireStatus(subscription, SubscriptionStatus.ACTIVE);
        requireField(currentDate, "currentDate");

        if (!currentDate.isAfter(subscription.getEffectiveTo())) {
            throw new ConflictException("Subscription has not expired yet");
        }

        subscription.setStatus(SubscriptionStatus.EXPIRED);
        validateState(subscription);
    }

    public void validateState(Subscription subscription) {
        requireSubscription(subscription);
        requireField(subscription.getSubscriptionId(), "subscriptionId");
        requireField(subscription.getCustomerId(), "customerId");
        requireField(subscription.getCustomerVehicleId(), "customerVehicleId");
        requireField(subscription.getTicketTypeId(), "ticketTypeId");
        requireField(subscription.getPriceRuleId(), "priceRuleId");
        requireField(subscription.getRequestedEffectiveFrom(), "requestedEffectiveFrom");
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

        switch (subscription.getStatus()) {
            case PENDING -> {
                requireNoApproval(subscription);
                requireNoRejection(subscription);
                requireNoCard(subscription);
            }
            case PENDING_PAYMENT, PENDING_CARD -> {
                requireApproval(subscription);
                requireNoRejection(subscription);
                requireField(subscription.getCardId(), "cardId");
                if (subscription.getCardReceiptDate() != null) {
                    throw new BadRequestException("Pending subscription must not have cardReceiptDate");
                }
            }
            case ACTIVE, EXPIRED -> {
                requireApproval(subscription);
                requireNoRejection(subscription);
                requireField(subscription.getCardId(), "cardId");
                requireField(subscription.getCardReceiptDate(), "cardReceiptDate");
            }
            case CANCELLED -> requireNoRejection(subscription);
            case REJECTED -> {
                requireNoApproval(subscription);
                requireNoCard(subscription);
                requireField(subscription.getRejectionReason(), "rejectionReason");
                requireField(subscription.getRejectedBy(), "rejectedBy");
                requireField(subscription.getRejectedAt(), "rejectedAt");
            }
        }
    }

    private void validateRequestedDate(LocalDate requestedDate, LocalDate currentDate) {
        requireField(requestedDate, "requestedEffectiveFrom");
        requireField(currentDate, "currentDate");

        LocalDate minDate = currentDate.plusDays(MIN_EFFECTIVE_LEAD_DAYS);
        LocalDate maxDate = currentDate.plusDays(MAX_EFFECTIVE_LEAD_DAYS);

        if (requestedDate.isBefore(minDate) || requestedDate.isAfter(maxDate)) {
            throw new BadRequestException("requestedEffectiveFrom must be between " + minDate + " and " + maxDate);
        }
    }

    private LocalDate calculateEffectiveTo(LocalDate effectiveFrom, int durationDays) {
        return effectiveFrom.plusDays(durationDays - 1L);
    }

    private void validateDuration(int durationDays) {
        if (durationDays <= 0) {
            throw new BadRequestException("durationDays must be greater than zero");
        }
    }

    private void requireApproval(Subscription subscription) {
        requireField(subscription.getApprovedBy(), "approvedBy");
        requireField(subscription.getApprovedAt(), "approvedAt");
    }

    private void requireNoApproval(Subscription subscription) {
        if (subscription.getApprovedBy() != null || subscription.getApprovedAt() != null) {
            throw new BadRequestException("Subscription must not contain approval metadata");
        }
    }


    private void requireNoRejection(Subscription subscription) {
        if (subscription.getRejectionReason() != null
                || subscription.getRejectedBy() != null
                || subscription.getRejectedAt() != null) {
            throw new BadRequestException("Subscription must not contain rejection metadata");
        }
    }

    private void requireNoCard(Subscription subscription) {
        if (subscription.getCardId() != null || subscription.getCardReceiptDate() != null) {
            throw new BadRequestException("Subscription must not contain card assignment");
        }
    }

    private void clearApproval(Subscription subscription) {
        subscription.setApprovedBy(null);
        subscription.setApprovedAt(null);
    }

    private void clearRejection(Subscription subscription) {
        subscription.setRejectionReason(null);
        subscription.setRejectedBy(null);
        subscription.setRejectedAt(null);
    }

    private void requireStatus(Subscription subscription, SubscriptionStatus expectedStatus) {
        requireSubscription(subscription);
        if (subscription.getStatus() != expectedStatus) {
            throw new ConflictException("Subscription must be in " + expectedStatus + " status");
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

