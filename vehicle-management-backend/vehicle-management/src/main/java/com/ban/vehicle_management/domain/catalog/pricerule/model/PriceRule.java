package com.ban.vehicle_management.domain.catalog.pricerule.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.PriceRuleUnit;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceRule extends AuditableDomainModel {

    private UUID priceRuleId;
    private UUID pricePlanId;
    private UUID vehicleTypeId;
    private UUID ticketTypeId;
    private String ruleName;
    private LocalTime timeFrom;
    private LocalTime timeTo;
    private BigDecimal basePrice;
    private PriceRuleUnit unit;
    private BigDecimal lostCardFee;
    private Integer priority;
}
