package com.ban.vehicle_management.domain.iam.organization.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Organization extends AuditableDomainModel {

    private UUID organizationId;
    private String code;
    private String name;
    private String address;
    private OrganizationStatus status;
}
