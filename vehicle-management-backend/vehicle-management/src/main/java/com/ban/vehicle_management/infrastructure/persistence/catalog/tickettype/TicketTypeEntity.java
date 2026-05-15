package com.ban.vehicle_management.infrastructure.persistence.catalog.tickettype;

import com.ban.vehicle_management.infrastructure.persistence.accesscontrol.subscription.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.catalog.pricerule.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_types", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeEntity extends AuditableEntity {

    @Id
    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "ticketType")
    private Set<PriceRuleEntity> priceRules = new HashSet<>();

    @OneToMany(mappedBy = "ticketType")
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

}
