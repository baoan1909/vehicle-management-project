package com.ban.vehicle_management.entrypoint.dto.catalog.pricerule.request;

import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record UpdatePriceRuleRequest(
        UUID vehicleTypeId,
        UUID ticketTypeId,
        String ruleName,
        LocalTime timeFrom,
        LocalTime timeTo,
        BigDecimal basePrice,
        PriceRuleUnit unit,
        BigDecimal lostCardFee,
        Integer priority
) {
}