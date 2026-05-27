package com.ban.vehicle_management.entrypoint.dto.catalog.priceplan.response;

import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PricePlanAdminResponse {
    private UUID pricePlanId;
    private String code;
    private String name;
    private String description;
    private PricePlanAppliesTo appliesTo;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}