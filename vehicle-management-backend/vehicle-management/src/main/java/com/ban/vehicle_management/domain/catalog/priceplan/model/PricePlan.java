package com.ban.vehicle_management.domain.catalog.priceplan.model;

import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;
import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.catalog.PricePlanAppliesTo;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricePlan extends AuditableDomainModel {

    private UUID pricePlanId;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private PricePlanAppliesTo appliesTo;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private CatalogLifecycleStatus lifecycleStatus;

    public CatalogLifecycleStatus getLifecycleStatus() {
        if (lifecycleStatus != null) return lifecycleStatus;
        return Boolean.TRUE.equals(isActive)
                ? CatalogLifecycleStatus.ACTIVE
                : CatalogLifecycleStatus.INACTIVE;
    }

    public void setLifecycleStatus(CatalogLifecycleStatus status) {
        lifecycleStatus = status;
        isActive = status == CatalogLifecycleStatus.ACTIVE;
    }
}

