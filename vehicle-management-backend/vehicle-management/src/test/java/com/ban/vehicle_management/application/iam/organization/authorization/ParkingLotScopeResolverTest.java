package com.ban.vehicle_management.application.iam.organization.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.port.out.ParkingLotAssignmentPortOut;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotManagementState;
import com.ban.vehicle_management.shared.exception.ScopedAccessDeniedException;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotScopeResolverTest {
    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private ParkingLotAssignmentPortOut assignmentPortOut;

    private ParkingLotScopeResolver resolver;
    private UUID accountId;

    @BeforeEach
    void setUp() {
        resolver = new ParkingLotScopeResolver(currentAccountPortIn, assignmentPortOut);
        accountId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(accountId);
    }

    @Test
    void outOfScopeLotIsRejectedBeforeItsStateIsRead() {
        UUID lotId = UUID.randomUUID();
        when(assignmentPortOut.findEmployeeReadableLotIds(any(), any())).thenReturn(Set.of());

        ScopedAccessDeniedException error = assertThrows(
                ScopedAccessDeniedException.class, () -> resolver.ensureEmployeeCanOperate(lotId));

        assertEquals("PARKING_LOT_OUT_OF_SCOPE", error.code());
        verify(assignmentPortOut, never()).findParkingLotManagementState(lotId);
    }

    @Test
    void suspendedLotHasAStableErrorCode() {
        UUID lotId = UUID.randomUUID();
        when(assignmentPortOut.findEmployeeReadableLotIds(any(), any())).thenReturn(Set.of(lotId));
        when(assignmentPortOut.findParkingLotManagementState(lotId))
                .thenReturn(ParkingLotManagementState.OPERATION_SUSPENDED);

        ScopedAccessDeniedException error = assertThrows(
                ScopedAccessDeniedException.class, () -> resolver.ensureEmployeeCanOperate(lotId));

        assertEquals("PARKING_LOT_OPERATION_SUSPENDED", error.code());
    }

    @Test
    void activeOpenShiftAllowsOperation() {
        UUID lotId = UUID.randomUUID();
        when(assignmentPortOut.findEmployeeReadableLotIds(any(), any())).thenReturn(Set.of(lotId));
        when(assignmentPortOut.findParkingLotManagementState(lotId))
                .thenReturn(ParkingLotManagementState.MANAGED);
        when(assignmentPortOut.findEmployeeOperableLotIds(any(), any())).thenReturn(Set.of(lotId));

        assertDoesNotThrow(() -> resolver.ensureEmployeeCanOperate(lotId));
    }
}
