package com.ban.vehicle_management.entrypoint.dto.iam.partnerregistration.response;

public record PartnerAddressResponse(
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDetail,
        String addressDisplay
) {
}
