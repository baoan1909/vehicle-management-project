package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformFeeLine {

    private UUID feeTypeId;
    private UUID ruleVersionId;
    private String feeCode;
    private String feeName;
    private int version;
    private BigDecimal feeRate;
    private PlatformFeeScopeType scopeType;
    private PlatformFeeTransactionType transactionType;
    private PlatformFeeCalculationType calculationType;
    private PlatformFeeBasis feeBasis;
    private PlatformFeeChargedTo chargedTo;
    private boolean stackable;
    private long basisAmount;
    private long feeAmount;

    public PlatformFeeLine(UUID feeTypeId, UUID ruleVersionId, String feeCode, String feeName,
            int version, BigDecimal feeRate, PlatformFeeBasis feeBasis,
            PlatformFeeChargedTo chargedTo, long basisAmount, long feeAmount) {
        this(feeTypeId, ruleVersionId, feeCode, feeName, version, feeRate,
                PlatformFeeScopeType.TRANSACTION_TYPE,
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE,
                PlatformFeeCalculationType.PERCENTAGE, feeBasis, chargedTo, true,
                basisAmount, feeAmount);
    }
}
