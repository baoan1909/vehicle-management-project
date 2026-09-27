package com.ban.vehicle_management.domain.common.licenseplate;

public record LicensePlateResolution(
        String normalized,
        String display,
        LicensePlateFormat format,
        boolean validFormat,
        boolean needsReview
) {
}
