package com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.response;

import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PriceRuleAdminResponse {
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
    private Boolean isActive;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}