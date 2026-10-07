package com.ban.vehicle_management.infrastructure.persistence.database.entity.iam;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organizations", schema = "iam")
@Getter
@Setter
@NoArgsConstructor
public class OrganizationEntity extends AuditableEntity {

    @Id
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "address_display")
    private String addressDisplay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrganizationStatus status;
}
