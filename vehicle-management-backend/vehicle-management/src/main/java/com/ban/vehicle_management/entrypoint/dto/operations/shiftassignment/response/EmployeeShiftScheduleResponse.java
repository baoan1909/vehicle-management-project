package com.ban.vehicle_management.entrypoint.dto.operations.shiftassignment.response;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftAssignmentStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeShiftScheduleResponse {
    private UUID shiftAssignmentId;
    private UUID shiftId;
    private UUID parkingLotId;
    private String parkingLotName;
    private String shiftCode;
    private Instant startTime;
    private Instant endTime;
    private ShiftStatus shiftStatus;
    private ShiftAssignmentStatus status;
}
