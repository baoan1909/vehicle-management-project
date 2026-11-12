package com.ban.vehicle_management.application.parking.checkinspacehold.port.in;

import com.ban.vehicle_management.application.parking.checkinspacehold.model.command.CreateCheckInSpaceHoldCommand;
import com.ban.vehicle_management.application.parking.checkinspacehold.model.result.CheckInSpaceHoldResult;
import java.time.Instant;
import java.util.UUID;

public interface CheckInSpaceHoldPortIn {
    CheckInSpaceHoldResult create(CreateCheckInSpaceHoldCommand command);
    void cancel(UUID holdId, String reason);
    int expireDue(Instant now);
}
