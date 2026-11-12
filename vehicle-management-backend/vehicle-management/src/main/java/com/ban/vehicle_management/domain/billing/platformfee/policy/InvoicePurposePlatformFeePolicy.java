package com.ban.vehicle_management.domain.billing.platformfee.policy;

import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.exception.BadRequestException;

/** Central mapping between invoice revenue purpose and platform-fee transaction type. */
public class InvoicePurposePlatformFeePolicy {

    public PlatformFeeTransactionType transactionTypeFor(InvoicePurpose purpose) {
        if (purpose == null) {
            throw new BadRequestException("Invoice purpose is required to resolve platform fees");
        }
        return switch (purpose) {
            case PARKING_SESSION -> PlatformFeeTransactionType.PARKING_SESSION;
            case SUBSCRIPTION_PURCHASE -> PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE;
            case SUBSCRIPTION_RENEWAL -> PlatformFeeTransactionType.SUBSCRIPTION_RENEWAL;
            case SUBSCRIPTION_CARD_DEPOSIT -> PlatformFeeTransactionType.SUBSCRIPTION_CARD_DEPOSIT;
            case LOST_CARD -> PlatformFeeTransactionType.LOST_CARD;
            case MANUAL -> PlatformFeeTransactionType.MANUAL;
            case RESERVATION_PREPAYMENT -> PlatformFeeTransactionType.RESERVATION_PREPAYMENT;
        };
    }
}
