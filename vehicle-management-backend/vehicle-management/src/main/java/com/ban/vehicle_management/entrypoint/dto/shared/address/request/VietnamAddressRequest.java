package com.ban.vehicle_management.entrypoint.dto.shared.address.request;

public record VietnamAddressRequest(
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDetail
) {
}
