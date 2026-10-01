package com.ban.vehicle_management.infrastructure.persistence.database.entity.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "legacy_provinces", schema = "reference")
@Getter
@NoArgsConstructor
public class LegacyProvinceEntity {
    @Id
    @Column(name = "code", nullable = false, length = 2)
    private String code;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "full_name", nullable = false)
    private String fullName;
}
