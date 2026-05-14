package com.ban.vehicle_management.domain.operations.shift.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.ShiftStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
    private String shiftCode;
    private UUID parkingLotId;
    private Instant startTime;
    private Instant endTime;
    private ShiftStatus status;
    private BigDecimal openingCash;
    private BigDecimal closingCash;
}
