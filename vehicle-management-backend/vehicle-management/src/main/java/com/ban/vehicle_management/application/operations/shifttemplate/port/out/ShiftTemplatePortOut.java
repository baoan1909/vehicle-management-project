package com.ban.vehicle_management.application.operations.shifttemplate.port.out;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftTemplatePortOut {

    ShiftTemplate save(ShiftTemplate shiftTemplate);

    Optional<ShiftTemplate> findById(UUID shiftTemplateId);

    List<ShiftTemplate> findAll(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            String keyword
    );

    List<ShiftTemplate> findActiveByParkingLotId(UUID parkingLotId);

    boolean existsActiveByParkingLotIdAndShiftType(
            UUID parkingLotId,
            ShiftType shiftType
    );

    boolean existsActiveByParkingLotIdAndShiftTypeAndIdNot(
            UUID parkingLotId,
            ShiftType shiftType,
            UUID shiftTemplateId
    );
}