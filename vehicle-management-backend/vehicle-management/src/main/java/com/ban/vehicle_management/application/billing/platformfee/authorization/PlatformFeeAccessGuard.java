package com.ban.vehicle_management.application.billing.platformfee.authorization;

import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import org.springframework.stereotype.Component;

@Component
public class PlatformFeeAccessGuard {

    public static final String CREATE_PERMISSION = "PLATFORM_FEE_CREATE_ALL";
    public static final String READ_PERMISSION = "PLATFORM_FEE_READ_ALL";
    public static final String UPDATE_PERMISSION = "PLATFORM_FEE_UPDATE_ALL";
    public static final String ACTIVATE_PERMISSION = "PLATFORM_FEE_ACTIVATE_ALL";
    public static final String ARCHIVE_PERMISSION = "PLATFORM_FEE_ARCHIVE_ALL";
    public static final String PREVIEW_PERMISSION = "PLATFORM_FEE_PREVIEW_ALL";

    private final CurrentAccountPortIn currentAccountPortIn;

    public PlatformFeeAccessGuard(CurrentAccountPortIn currentAccountPortIn) {
        this.currentAccountPortIn = currentAccountPortIn;
    }

    public void ensureCanCreate() {
        currentAccountPortIn.requirePermission(CREATE_PERMISSION);
    }

    public void ensureCanRead() {
        currentAccountPortIn.requirePermission(READ_PERMISSION);
    }

    public void ensureCanUpdate() {
        currentAccountPortIn.requirePermission(UPDATE_PERMISSION);
    }

    public void ensureCanActivate() {
        currentAccountPortIn.requirePermission(ACTIVATE_PERMISSION);
    }

    public void ensureCanArchive() {
        currentAccountPortIn.requirePermission(ARCHIVE_PERMISSION);
    }

    public void ensureCanPreview() {
        currentAccountPortIn.requirePermission(PREVIEW_PERMISSION);
    }
}
