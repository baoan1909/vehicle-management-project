package com.ban.vehicle_management.infrastructure.persistence.people.customer;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customers", schema = "people")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerEntity extends AuditableEntity {

    @Id
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "user_profile_id", nullable = false, unique = true)
    private UUID userProfileId;

    @Column(name = "customer_code", nullable = false, unique = true)
    private String customerCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private CustomerType customerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private CustomerApprovalStatus approvalStatus;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

}
