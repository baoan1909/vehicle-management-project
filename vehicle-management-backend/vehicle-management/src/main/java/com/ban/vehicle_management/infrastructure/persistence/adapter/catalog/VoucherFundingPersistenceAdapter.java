package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.voucher.port.out.VoucherFundingPortOut;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.infrastructure.mapper.billing.WalletPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.billing.VoucherFinancialTermRepository;
import org.springframework.stereotype.Component;

@Component
public class VoucherFundingPersistenceAdapter implements VoucherFundingPortOut {
    private final VoucherFinancialTermRepository repository;
    private final WalletPersistenceMapper mapper;

    public VoucherFundingPersistenceAdapter(
            VoucherFinancialTermRepository repository,
            WalletPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public VoucherFinancialTerm saveFundingTerm(VoucherFinancialTerm term) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(term)));
    }
}
