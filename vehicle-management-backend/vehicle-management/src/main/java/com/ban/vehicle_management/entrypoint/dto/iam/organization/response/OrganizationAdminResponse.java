package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import java.time.Instant;

import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationAdminResponse {
    private UUID organizationId;
    private String code;
    private String name;
    private String address;
    private OrganizationStatus status;
    private Instant createdAt;
    private UUID createdBy;
    private Instant updatedAt;
    private UUID updatedBy;
}
