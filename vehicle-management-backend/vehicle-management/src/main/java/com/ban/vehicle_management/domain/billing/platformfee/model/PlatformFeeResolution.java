package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
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

    private PlatformFeeTransactionType transactionType;
    private Instant effectiveAt;
    private long grossAmount;
    private long customerPaidAmount;
    private List<PlatformFeeLine> feeLines;
    private long totalFeeAmount;
    private BigDecimal effectiveTotalRate;
}
