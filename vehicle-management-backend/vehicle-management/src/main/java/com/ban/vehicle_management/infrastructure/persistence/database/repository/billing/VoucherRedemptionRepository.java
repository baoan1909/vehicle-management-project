package com.ban.vehicle_management.infrastructure.persistence.database.repository.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.billing.VoucherRedemptionEntity;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemptionEntity, UUID> {
    Optional<VoucherRedemptionEntity> findBySubscriptionId(UUID subscriptionId);
    long countByVoucherIdAndStatusIn(UUID voucherId, Collection<VoucherRedemptionStatus> statuses);
    long countByVoucherIdAndCustomerIdAndStatusIn(UUID voucherId, UUID customerId, Collection<VoucherRedemptionStatus> statuses);
}
