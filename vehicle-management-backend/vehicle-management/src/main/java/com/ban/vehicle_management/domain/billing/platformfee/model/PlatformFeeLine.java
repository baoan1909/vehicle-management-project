package com.ban.vehicle_management.domain.billing.platformfee.model;

import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
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
    private PlatformFeeBasis feeBasis;
    private PlatformFeeChargedTo chargedTo;
    private long basisAmount;
    private long feeAmount;
}
