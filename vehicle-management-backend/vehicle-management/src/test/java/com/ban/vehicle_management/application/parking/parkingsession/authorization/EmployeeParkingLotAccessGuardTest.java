package com.ban.vehicle_management.application.parking.parkingsession.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.ParkingLotScopeResolver;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class EmployeeParkingLotAccessGuardTest {

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private ParkingLotScopeResolver parkingLotScopeResolver;
    @InjectMocks private EmployeeParkingLotAccessGuard guard;

    @Test
    void activeShiftAllowsOnlyItsParkingLot() {
        UUID lotId = UUID.randomUUID();
        UUID anotherLotId = UUID.randomUUID();
        doNothing().when(parkingLotScopeResolver).ensureEmployeeCanOperate(lotId);
        doThrow(new AccessDeniedException("ACTIVE_SHIFT_REQUIRED"))
                .when(parkingLotScopeResolver).ensureEmployeeCanOperate(anotherLotId);

        assertDoesNotThrow(() -> guard.ensureCanOperate(lotId));
        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(anotherLotId));
    }

    @Test
    void noActiveShiftDeniesOperation() {
        UUID lotId = UUID.randomUUID();
        doThrow(new AccessDeniedException("ACTIVE_SHIFT_REQUIRED"))
                .when(parkingLotScopeResolver).ensureEmployeeCanOperate(lotId);

        assertThrows(AccessDeniedException.class, () -> guard.ensureCanOperate(lotId));
    }
}
