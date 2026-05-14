package com.ban.vehicle_management.domain.common.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditableDomainModel {

    private Instant createdAt;
    private UUID createdBy;

    private Instant updatedAt;
    private UUID updatedBy;
}
