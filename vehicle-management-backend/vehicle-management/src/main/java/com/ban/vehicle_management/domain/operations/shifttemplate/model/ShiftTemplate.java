package com.ban.vehicle_management.domain.operations.shifttemplate.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTemplate extends AuditableDomainModel {

    private UUID shiftTemplateId;
    private UUID parkingLotId;
    private ShiftType shiftType;
    private String name;
    private LocalTime startLocalTime;
    private LocalTime endLocalTime;
    private ShiftTemplateStatus status;
}