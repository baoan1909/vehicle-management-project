package com.ban.vehicle_management.infrastructure.persistence.database.repository.parking;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.parking.GateEntity;
import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GateRepository extends JpaRepository<GateEntity, UUID>, JpaSpecificationExecutor<GateEntity> {

    boolean existsByZoneIdAndStatus(UUID zoneId, GateStatus status);

    boolean existsByZoneIdAndCode(UUID zoneId, String code);

    boolean existsByZoneIdAndCodeAndGateIdNot(UUID zoneId, String code, UUID gateId);
}