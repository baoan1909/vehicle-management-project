package com.ban.vehicle_management.infrastructure.persistence.adapter.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.people.EmployeeParkingLotAssignmentRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeePaymentScopePersistenceAdapterTest {
    @Mock EmployeeParkingLotAssignmentRepository assignmentRepository;
    @InjectMocks EmployeePaymentScopePersistenceAdapter adapter;

    @Test
    void paymentsUseTimeBoundOperationalAssignmentScope() {
        UUID accountId = UUID.randomUUID();
        Set<UUID> lots = Set.of(UUID.randomUUID());
        when(assignmentRepository.findOperableParkingLotIds(eq(accountId), any())).thenReturn(lots);
        Instant before = Instant.now();
        assertEquals(lots, adapter.findOpenShiftParkingLotIds(accountId));
        ArgumentCaptor<Instant> timestamp = ArgumentCaptor.forClass(Instant.class);
        verify(assignmentRepository).findOperableParkingLotIds(eq(accountId), timestamp.capture());
        assertFalse(timestamp.getValue().isBefore(before));
        assertTrue(!timestamp.getValue().isAfter(Instant.now()));
    }
}
