package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.ShiftTemplateEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ShiftTemplateRepository
        extends JpaRepository<ShiftTemplateEntity, UUID>,
        JpaSpecificationExecutor<ShiftTemplateEntity> {

    boolean existsByParkingLotIdAndShiftTypeAndStatus(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status
    );

    boolean existsByParkingLotIdAndShiftTypeAndStatusAndShiftTemplateIdNot(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            UUID shiftTemplateId
    );

    List<ShiftTemplateEntity> findAllByParkingLotIdAndStatus(
            UUID parkingLotId,
            ShiftTemplateStatus status
    );
}