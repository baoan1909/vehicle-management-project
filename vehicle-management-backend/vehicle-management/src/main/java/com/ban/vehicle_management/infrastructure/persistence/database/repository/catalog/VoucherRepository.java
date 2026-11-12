package com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.catalog.VoucherEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;

public interface VoucherRepository extends JpaRepository<VoucherEntity, UUID> {
    Optional<VoucherEntity> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndVoucherIdNot(String code, UUID voucherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select voucher from VoucherEntity voucher where upper(voucher.code) = upper(:code)")
    Optional<VoucherEntity> findByCodeForUpdate(@Param("code") String code);

    @Modifying
    @Query("""
        update VoucherEntity voucher set voucher.status =
            com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus.EXPIRED
        where voucher.organizationId in :organizationIds
          and voucher.status <> com.ban.vehicle_management.shared.enumeration.catalog.VoucherStatus.EXPIRED
        """)
    int expireAllByOrganizationIds(@Param("organizationIds") Collection<UUID> organizationIds);
}
