package com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.accesscontrol.SubscriptionEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.PriceRuleEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import jakarta.persistence.*;

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

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketTypeStatus status;

    @OneToMany(mappedBy = "ticketType")
    private Set<PriceRuleEntity> priceRules = new HashSet<>();

    @OneToMany(mappedBy = "ticketType")
    private Set<SubscriptionEntity> subscriptions = new HashSet<>();

}


