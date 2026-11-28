package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import com.ban.vehicle_management.application.billing.payment.port.out.EmployeePaymentScopePortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmployeePaymentScopePersistenceAdapter implements EmployeePaymentScopePortOut {
    private final EmployeeParkingLotAssignmentRepository assignmentRepository;

    public EmployeePaymentScopePersistenceAdapter(EmployeeParkingLotAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Set<UUID> findOpenShiftParkingLotIds(UUID accountId) {
        return assignmentRepository.findOperableParkingLotIds(accountId, Instant.now());
    }
}
