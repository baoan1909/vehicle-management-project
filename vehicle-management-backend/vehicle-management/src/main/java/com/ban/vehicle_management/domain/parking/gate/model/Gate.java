package com.ban.vehicle_management.domain.parking.gate.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Gate extends AuditableDomainModel {

    private UUID gateId;
    private UUID zoneId;
    private String code;
    private String name;
    private GateStatus status;
}