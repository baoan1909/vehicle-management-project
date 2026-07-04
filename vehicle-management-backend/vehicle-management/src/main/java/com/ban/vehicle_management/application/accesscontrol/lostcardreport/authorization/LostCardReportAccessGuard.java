package com.ban.vehicle_management.application.accesscontrol.lostcardreport.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.springframework.stereotype.Component;

@Component
public class LostCardReportAccessGuard {

    private static final String CREATE_ALL = "LOST_CARD_REPORT_CREATE_ALL";
    private static final String READ_ALL = "LOST_CARD_REPORT_READ_ALL";
    private static final String UPDATE_ALL = "LOST_CARD_REPORT_UPDATE_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public LostCardReportAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public void ensureCanCreate() {
        currentAccountPortIn.requirePermission(CREATE_ALL);
    }

    public void ensureCanRead() {
        currentAccountPortIn.requirePermission(READ_ALL);
    }

    public void ensureCanUpdate() {
        currentAccountPortIn.requirePermission(UPDATE_ALL);
    }
}
