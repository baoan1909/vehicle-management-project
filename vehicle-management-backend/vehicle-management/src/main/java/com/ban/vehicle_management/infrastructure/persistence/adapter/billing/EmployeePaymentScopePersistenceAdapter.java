package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.payment.port.out.EmployeePaymentScopePortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.ShiftAssignmentRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmployeePaymentScopePersistenceAdapter implements EmployeePaymentScopePortOut {
    private final ShiftAssignmentRepository shiftAssignmentRepository;

    public EmployeePaymentScopePersistenceAdapter(ShiftAssignmentRepository shiftAssignmentRepository) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
    }

    @Override
    public Set<UUID> findOpenShiftParkingLotIds(UUID accountId) {
        return Set.copyOf(shiftAssignmentRepository.findOpenShiftParkingLotIdsByAccountId(accountId));
    }
}
