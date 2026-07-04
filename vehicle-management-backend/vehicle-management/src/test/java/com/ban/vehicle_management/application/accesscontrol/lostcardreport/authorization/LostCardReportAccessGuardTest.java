package com.ban.vehicle_management.application.accesscontrol.lostcardreport.authorization;

import static org.mockito.Mockito.verify;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LostCardReportAccessGuardTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private LostCardReportAccessGuard accessGuard;

    @Test
    void shouldRequireCreatePermission() {
        accessGuard.ensureCanCreate();

        verify(currentAccountPortIn).requirePermission("LOST_CARD_REPORT_CREATE_ALL");
    }

    @Test
    void shouldRequireReadPermission() {
        accessGuard.ensureCanRead();

        verify(currentAccountPortIn).requirePermission("LOST_CARD_REPORT_READ_ALL");
    }

    @Test
    void shouldRequireUpdatePermission() {
        accessGuard.ensureCanUpdate();

        verify(currentAccountPortIn).requirePermission("LOST_CARD_REPORT_UPDATE_ALL");
    }
}
