package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.exception.BadRequestException;

/** Explicit collector policy; payment completion never accepts custody from the client. */
public class FundCustodyPolicy {

    public FundCustody resolve(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new BadRequestException("paymentMethod is required to resolve fund custody");
        }
        return switch (paymentMethod) {
            case CASH, BANK_TRANSFER -> FundCustody.PARTNER;
            case QR, MOMO, VNPAY, SEPAY, WALLET -> FundCustody.PLATFORM;
        };
    }

    public FundCustody resolveSnapshot(FundCustody snapshottedCustody, PaymentMethod legacyPaymentMethod) {
        return snapshottedCustody == null ? resolve(legacyPaymentMethod) : snapshottedCustody;
    }
}
