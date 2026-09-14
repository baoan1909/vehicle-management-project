package com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VoucherCustomerBannerResponse {
    private String code;
    private String bannerTitle;
    private String bannerDescription;
    private Integer bannerPriority;
    private boolean showOnDashboard;
    private boolean showOnSubscriptionPage;
}
