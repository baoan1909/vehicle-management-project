package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import com.ban.vehicle_management.shared.enumeration.iam.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationProfileResponse {
    private UUID organizationId;
    private String code;
    private String name;
    private String addressDetail;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String addressDisplay;
    private OrganizationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String representativeUsername;
    private String representativeEmail;
    private long totalParkingLots;
    private long activeParkingLots;
    private long setupParkingLots;
    private long maintenanceParkingLots;
    private OrganizationProfileUpdateRequestResponse latestUpdateRequest;
}
