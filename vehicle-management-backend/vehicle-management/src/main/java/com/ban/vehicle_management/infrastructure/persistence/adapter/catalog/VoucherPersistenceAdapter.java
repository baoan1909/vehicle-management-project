package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.voucher.port.out.VoucherPortOut;
import com.ban.vehicle_management.domain.billing.voucherredemption.model.VoucherRedemption;
import com.ban.vehicle_management.domain.catalog.voucher.model.Voucher;
import com.ban.vehicle_management.infrastructure.mapper.billing.VoucherRedemptionPersistenceMapper;
import com.ban.vehicle_management.infrastructure.mapper.catalog.VoucherPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.VoucherRedemptionRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VoucherRepository;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VoucherPersistenceAdapter implements VoucherPortOut {
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final VoucherPersistenceMapper voucherPersistenceMapper;
    private final VoucherRedemptionPersistenceMapper voucherRedemptionPersistenceMapper;

    public VoucherPersistenceAdapter(VoucherRepository voucherRepository, VoucherRedemptionRepository voucherRedemptionRepository, VoucherPersistenceMapper voucherPersistenceMapper, VoucherRedemptionPersistenceMapper voucherRedemptionPersistenceMapper) {
        this.voucherRepository = voucherRepository;
        this.voucherRedemptionRepository = voucherRedemptionRepository;
        this.voucherPersistenceMapper = voucherPersistenceMapper;
        this.voucherRedemptionPersistenceMapper = voucherRedemptionPersistenceMapper;
    }
    @Override public Voucher saveVoucher(Voucher voucher) { return voucherPersistenceMapper.toDomain(voucherRepository.saveAndFlush(voucherPersistenceMapper.toEntity(voucher))); }
    @Override public Optional<Voucher> findVoucherById(UUID voucherId) { return voucherRepository.findById(voucherId).map(voucherPersistenceMapper::toDomain); }
    @Override public Optional<Voucher> findVoucherByCode(String code) { return voucherRepository.findByCodeIgnoreCase(code).map(voucherPersistenceMapper::toDomain); }
    @Override public Optional<Voucher> findVoucherByCodeForUpdate(String code) { return voucherRepository.findByCodeForUpdate(code).map(voucherPersistenceMapper::toDomain); }
    @Override public boolean existsVoucherByCodeAndVoucherIdNot(String code, UUID voucherId) { return voucherRepository.existsByCodeIgnoreCaseAndVoucherIdNot(code, voucherId); }
    @Override public List<Voucher> findAllVouchers() { return voucherPersistenceMapper.toDomains(voucherRepository.findAll()); }
    @Override public VoucherRedemption saveRedemption(VoucherRedemption redemption) { return voucherRedemptionPersistenceMapper.toDomain(voucherRedemptionRepository.saveAndFlush(voucherRedemptionPersistenceMapper.toEntity(redemption))); }
    @Override public Optional<VoucherRedemption> findRedemptionBySubscriptionId(UUID subscriptionId) { return voucherRedemptionRepository.findBySubscriptionId(subscriptionId).map(voucherRedemptionPersistenceMapper::toDomain); }
    @Override public long countRedemptionsByVoucherIdAndStatusIn(UUID voucherId, Collection<VoucherRedemptionStatus> statuses) { return voucherRedemptionRepository.countByVoucherIdAndStatusIn(voucherId, statuses); }
    @Override public long countRedemptionsByVoucherIdAndCustomerIdAndStatusIn(UUID voucherId, UUID customerId, Collection<VoucherRedemptionStatus> statuses) { return voucherRedemptionRepository.countByVoucherIdAndCustomerIdAndStatusIn(voucherId, customerId, statuses); }
}
