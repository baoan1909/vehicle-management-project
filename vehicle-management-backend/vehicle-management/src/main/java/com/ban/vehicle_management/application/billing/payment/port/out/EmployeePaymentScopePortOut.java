package com.ban.vehicle_management.application.billing.payment.port.out;

import java.util.Set;
import java.util.UUID;

public interface EmployeePaymentScopePortOut {
    Set<UUID> findOpenShiftParkingLotIds(UUID accountId);
}
