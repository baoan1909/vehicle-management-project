package com.ban.vehicle_management.domain.operations.shift.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ShiftType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shift extends AuditableDomainModel {

    private UUID shiftId;
    private UUID shiftTemplateId;
    private UUID parkingLotId;
    private String shiftCode;
    private LocalDate shiftDate;
    private ShiftType shiftType;
    private Instant startTime;
    private Instant endTime;
    private ShiftStatus status;
    private Instant approvedAt;
    private UUID approvedBy;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
    private Instant openedAt;
    private UUID openedBy;
    private Instant closedAt;
    private UUID closedBy;
    private Instant cancelledAt;
    private UUID cancelledBy;
    private String cancellationReason;
    private String note;
}