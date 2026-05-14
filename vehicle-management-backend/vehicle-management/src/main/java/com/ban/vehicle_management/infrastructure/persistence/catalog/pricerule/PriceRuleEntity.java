package com.ban.vehicle_management.infrastructure.persistence.catalog.pricerule;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.PriceRuleUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_rules", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceRuleEntity extends AuditableEntity {

    @Id
    @Column(name = "price_rule_id", nullable = false)
    private UUID priceRuleId;

    @Column(name = "price_plan_id", nullable = false)
    private UUID pricePlanId;

    @Column(name = "vehicle_type_id", nullable = false)
    private UUID vehicleTypeId;

    @Column(name = "ticket_type_id")
    private UUID ticketTypeId;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "time_from")
    private LocalTime timeFrom;

    @Column(name = "time_to")
    private LocalTime timeTo;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false)
    private PriceRuleUnit unit;

    @Column(name = "lost_card_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal lostCardFee;

    @Column(name = "priority", nullable = false)
    private Integer priority;

}
