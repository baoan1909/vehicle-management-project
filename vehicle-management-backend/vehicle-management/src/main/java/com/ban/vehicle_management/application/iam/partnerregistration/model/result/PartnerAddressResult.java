package com.ban.vehicle_management.application.iam.partnerregistration.model.result;

public record PartnerAddressResult(
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDetail,
        String addressDisplay
) {
}
