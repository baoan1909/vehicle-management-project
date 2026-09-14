package com.ban.vehicle_management.domain.catalog.voucher.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Voucher extends AuditableDomainModel {

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
    private Instant validFrom;
    private Instant validTo;
    private boolean showOnDashboard;
    private boolean showOnSubscriptionPage;
    private String bannerTitle;
    private String bannerDescription;
    private Integer bannerPriority;
    private VoucherStatus status;
}
