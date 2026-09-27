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
    private java.time.Instant startTime;
    private java.time.Instant endTime;
    private ShiftStatus status;

    private java.time.Instant approvedAt;
    private UUID approvedBy;

    private BigDecimal openingCash;
    private BigDecimal closingCash;

    private java.time.Instant openedAt;
    private UUID openedBy;

    private java.time.Instant closedAt;
    private UUID closedBy;

    private java.time.Instant cancelledAt;
    private UUID cancelledBy;
    private String cancellationReason;

    private String note;

    private java.time.Instant createdAt;
    private UUID createdBy;
    private java.time.Instant updatedAt;
    private UUID updatedBy;
}
