package com.ban.vehicle_management.entrypoint.dto.catalog.voucher.response;

import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VoucherAdminResponse {
    private UUID voucherId;
    private String code;
    private String name;
    private String description;
    private VoucherDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minimumSubscriptionAmount;
    private Integer maxRedemptions;
    private Integer maxRedemptionsPerCustomer;
    private String validFrom;
    private String validTo;
    private boolean showOnDashboard;
    private boolean showOnSubscriptionPage;
    private String bannerTitle;
    private String bannerDescription;
    private Integer bannerPriority;
    private VoucherStatus status;
    private String createdAt;
    private String updatedAt;
}
