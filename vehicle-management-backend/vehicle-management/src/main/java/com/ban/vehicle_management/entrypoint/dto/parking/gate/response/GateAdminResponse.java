package com.ban.vehicle_management.entrypoint.dto.parking.gate.response;

import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GateAdminResponse {
    private UUID gateId;
    private UUID zoneId;
    private String code;
    private String name;
    private GateStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}