package com.ban.vehicle_management.entrypoint.dto.operations.shifttemplate.response;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftTemplateStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftTemplateAdminResponse {

    private UUID shiftTemplateId;
    private UUID parkingLotId;
    private ShiftType shiftType;
    private String name;
    private LocalTime startLocalTime;
    private LocalTime endLocalTime;
    private ShiftTemplateStatus status;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}