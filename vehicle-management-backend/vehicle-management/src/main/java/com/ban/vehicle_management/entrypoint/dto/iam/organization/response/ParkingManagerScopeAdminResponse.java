package com.ban.vehicle_management.entrypoint.dto.iam.organization.response;

import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParkingManagerScopeAdminResponse {

    private Set<UUID> parkingManagerAccountIds;
}
