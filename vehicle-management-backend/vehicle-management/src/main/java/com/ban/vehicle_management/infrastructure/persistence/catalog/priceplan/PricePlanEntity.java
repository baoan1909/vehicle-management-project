package com.ban.vehicle_management.infrastructure.persistence.catalog.priceplan;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.catalog.pricerule.PriceRuleEntity;
import com.ban.vehicle_management.shared.enumeration.PricePlanAppliesTo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_plans", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricePlanEntity extends AuditableEntity {

    @Id
    @Column(name = "price_plan_id", nullable = false)
    private UUID pricePlanId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "applies_to", nullable = false)
    private PricePlanAppliesTo appliesTo;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "pricePlan")
    private Set<PriceRuleEntity> priceRules = new HashSet<>();

}
