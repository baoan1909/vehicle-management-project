package com.ban.vehicle_management.application.operations.shifttemplate.port.in;

import com.ban.vehicle_management.domain.operations.shifttemplate.model.ShiftTemplate;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.util.List;
import java.util.UUID;

public interface ShiftTemplatePortIn {

    ShiftTemplate createShiftTemplate(ShiftTemplate shiftTemplate);

    ShiftTemplate getShiftTemplateById(UUID shiftTemplateId);

    List<ShiftTemplate> getShiftTemplates(
            UUID parkingLotId,
            ShiftType shiftType,
            ShiftTemplateStatus status,
            String keyword
    );

    ShiftTemplate updateShiftTemplate(
            UUID shiftTemplateId,
            ShiftTemplate shiftTemplate
    );

    ShiftTemplate activateShiftTemplate(UUID shiftTemplateId);

    void deleteShiftTemplate(UUID shiftTemplateId);
}