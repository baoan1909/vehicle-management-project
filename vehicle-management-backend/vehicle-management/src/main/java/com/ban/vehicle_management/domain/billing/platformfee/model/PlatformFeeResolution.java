package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeResolution {

    private PlatformFeeTransactionType requestedTransactionType;
    private PlatformFeeScopeType resolvedScope;
    private boolean fallbackApplied;
    private boolean configurationFound;
    private Instant effectiveAt;
    private long grossAmount;
    private long customerPaidAmount;
    private List<PlatformFeeLine> feeLines;
    private long totalFeeAmount;
    private BigDecimal effectiveTotalRate;

    public PlatformFeeResolution(PlatformFeeTransactionType transactionType, Instant effectiveAt,
            long grossAmount, long customerPaidAmount, List<PlatformFeeLine> feeLines,
            long totalFeeAmount, BigDecimal effectiveTotalRate) {
        this(transactionType, PlatformFeeScopeType.TRANSACTION_TYPE, false,
                feeLines != null && !feeLines.isEmpty(), effectiveAt, grossAmount,
                customerPaidAmount, feeLines, totalFeeAmount, effectiveTotalRate);
    }
}
