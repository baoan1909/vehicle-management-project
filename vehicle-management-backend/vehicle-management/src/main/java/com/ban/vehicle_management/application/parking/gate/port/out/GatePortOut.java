package com.ban.vehicle_management.application.parking.gate.port.out;

import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GatePortOut {

    Gate save(Gate gate);

    Optional<Gate> findById(UUID gateId);

    List<Gate> findAll(UUID zoneId, GateStatus status, String keyword);

    boolean existsActiveZoneById(UUID zoneId);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndGateIdNot(UUID zoneId, String code, UUID gateId);

    boolean hasActiveLanes(UUID gateId);
}