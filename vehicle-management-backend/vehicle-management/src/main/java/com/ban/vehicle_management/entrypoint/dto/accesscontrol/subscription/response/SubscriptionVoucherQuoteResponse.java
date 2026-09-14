package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionVoucherQuoteResponse {

    private String voucherCode;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
}
