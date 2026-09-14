package com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vouchers", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherEntity extends AuditableEntity {
    @Id
    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "description")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private VoucherDiscountType discountType;
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;
    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;
    @Column(name = "minimum_subscription_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumSubscriptionAmount;
    @Column(name = "max_redemptions")
    private Integer maxRedemptions;
    @Column(name = "max_redemptions_per_customer", nullable = false)
    private Integer maxRedemptionsPerCustomer;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_to", nullable = false)
    private Instant validTo;
    @Column(name = "show_on_dashboard", nullable = false)
    private boolean showOnDashboard;
    @Column(name = "show_on_subscription_page", nullable = false)
    private boolean showOnSubscriptionPage;
    @Column(name = "banner_title", length = 150)
    private String bannerTitle;
    @Column(name = "banner_description", length = 500)
    private String bannerDescription;
    @Column(name = "banner_priority", nullable = false)
    private Integer bannerPriority;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherStatus status;
}
