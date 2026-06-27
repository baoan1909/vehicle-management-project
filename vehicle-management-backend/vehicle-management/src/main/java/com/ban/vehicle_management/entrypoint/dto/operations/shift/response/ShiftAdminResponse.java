package com.ban.vehicle_management.entrypoint.dto.operations.shift.response;

import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShiftAdminResponse {

    private UUID shiftId;
    private UUID shiftTemplateId;
    private UUID parkingLotId;
    private String shiftCode;
    private LocalDate shiftDate;
    private ShiftType shiftType;
    private String startTime;
    private String endTime;
    private ShiftStatus status;

    private String approvedAt;
    private UUID approvedBy;

    private BigDecimal openingCash;
    private BigDecimal closingCash;

    private String openedAt;
    private UUID openedBy;

    private String closedAt;
    private UUID closedBy;

    private String cancelledAt;
    private UUID cancelledBy;
    private String cancellationReason;

    private String note;

    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
}