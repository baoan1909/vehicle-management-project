package com.ban.vehicle_management.application.billing.platformfee.model.result;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformFeePreviewResult(
        PlatformFeeTransactionType transactionType,
        Instant effectiveAt,
        long grossAmount,
        long customerPaidAmount,
        List<PlatformFeePreviewLineResult> feeLines,
        long totalFeeAmount,
        BigDecimal effectiveTotalRate,
        long partnerRemainingAmount
) {

    public record PlatformFeePreviewLineResult(
            UUID feeTypeId,
            UUID ruleVersionId,
            String feeCode,
            String feeName,
            int version,
            BigDecimal feeRate,
            String feeBasis,
            String chargedTo,
            long basisAmount,
            long feeAmount
    ) {
    }
}
